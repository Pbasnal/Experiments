package org.example.domain;

import java.io.InputStream;

public record PageAsset(String mediaType, InputStream stream) {}
