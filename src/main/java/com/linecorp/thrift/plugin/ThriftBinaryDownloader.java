/*
 * Copyright 2023 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.thrift.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

public class ThriftBinaryDownloader {

    private final Logger logger;
    private final String repository;
    private final String version;
    private final File localBinaryDir;

    public ThriftBinaryDownloader(Logger logger, String repository, String version, File localBinaryDir) {
        this.logger = logger;
        this.repository = repository;
        this.version = version;
        this.localBinaryDir = localBinaryDir;
    }

    public File downloadBinary() {
        final String platform = detectPlatform();
        final String binaryName = "thrift." + platform + (isWindows() ? ".exe" : "");
        final File versionDir = new File(localBinaryDir, version);
        final File localBinary = new File(versionDir, binaryName);

        if (localBinary.exists()) {
            logger.info("Thrift binary already exists at: {}", localBinary.getAbsolutePath());
            return localBinary;
        }

        logger.info("Downloading thrift binary for platform: {}", platform);

        if (!versionDir.exists() && !versionDir.mkdirs()) {
            throw new GradleException("Failed to create directory: " + versionDir.getAbsolutePath());
        }

        final String downloadUrl = repository + '/' + version + '/' + binaryName;

        try {
            downloadFile(downloadUrl, localBinary);
            setExecutablePermissions(localBinary);
            logger.info("Successfully downloaded thrift binary to: {}", localBinary.getAbsolutePath());
            return localBinary;
        } catch (IOException e) {
            throw new GradleException("Failed to download thrift binary from: " + downloadUrl, e);
        }
    }

    private String detectPlatform() {
        final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        final String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

        final String osName;
        if (os.contains("win")) {
            osName = "windows";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osName = "osx";
        } else if (os.contains("linux")) {
            osName = "linux";
        } else {
            throw new GradleException("Unsupported operating system: " + os);
        }

        final String archName;
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            archName = "aarch_64";
        } else if (arch.contains("x86_64") || arch.contains("amd64")) {
            archName = "x86_64";
        } else {
            throw new GradleException("Unsupported architecture: " + arch);
        }

        return osName + '-' + archName;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    private static void downloadFile(String url, File destination) throws IOException {
        final URL downloadUrl = new URL(url);
        final Path destinationPath = destination.toPath();

        try (InputStream inputStream = new BufferedInputStream(downloadUrl.openStream())) {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void setExecutablePermissions(File file) {
        if (!isWindows()) {
            if (!file.setExecutable(true)) {
                logger.warn("Failed to set executable permissions for: {}", file.getAbsolutePath());
            }
        }
    }
}
