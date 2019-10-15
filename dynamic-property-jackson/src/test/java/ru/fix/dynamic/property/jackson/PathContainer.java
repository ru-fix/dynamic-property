package ru.fix.dynamic.property.jackson;

import java.nio.file.Path;

public class PathContainer {
    private Path path;

    public PathContainer() {
        //default ctor
    }

    public PathContainer(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
