package com.example.guestbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Shared secret a submission must include to be accepted. */
    private String submissionPasscode;

    /** Comma-separated origins allowed to call /api/** in dev. */
    private String corsAllowedOrigins = "http://localhost:4200";

    public String getSubmissionPasscode() { return submissionPasscode; }
    public void setSubmissionPasscode(String v) { this.submissionPasscode = v; }

    public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(String v) { this.corsAllowedOrigins = v; }
}