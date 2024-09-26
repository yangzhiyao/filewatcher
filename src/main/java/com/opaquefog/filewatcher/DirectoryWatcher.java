package com.opaquefog.filewatcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryWatcher {

    public static void main(String[] args) {
        Path dir = Paths.get("c:\\tmp");

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // 注册目录及其子目录
            registerAll(dir, watchService);

            // 无限循环等待变化事件
            WatchKey key;
            while (true) {
                try {
                    key = watchService.take(); // 阻塞等待变化事件
                } catch (InterruptedException x) {
                    x.printStackTrace(System.err);
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // 忽略 OVERFLOW 事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    // 事件的文件或目录名
                    Path fileName = (Path) event.context();

                    // 事件发生的目录
                    Path dirPath = (Path) key.watchable();
                    Path fullPath = dirPath;
                    if (Files.isDirectory(dirPath)) {
                        fullPath = dirPath.resolve(fileName);
                    }
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                        // 如果是创建目录事件，则注册该目录
                        registerAll(fullPath, watchService);
                    }

                    // 输出事件信息，如果是子目录下的文件或目录，输出完整路径
                    System.out.format("%s: %s\n", kind.name(), fullPath);
                }

                // 重置 key
                key.reset();
            }
        } catch (IOException x) {
            x.printStackTrace(System.err);
        }
    }

    // 递归注册目录及其子目录
    private static void registerAll(final Path start, final WatchService watcher) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}