package com.sun.fortress.tests.performance;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilenameFilter implements FilenameFilter {

    final private String extension;

    public ExtensionFilenameFilter(String extension) {
        this.extension = extension;
    }

    public boolean accept(File dir, String name) {
        return name.endsWith(extension);
    }

}
