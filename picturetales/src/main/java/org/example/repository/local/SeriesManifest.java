package org.example.repository.local;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeriesManifest {

    public String title;
    public String description;
    public String status = "UNKNOWN";
}
