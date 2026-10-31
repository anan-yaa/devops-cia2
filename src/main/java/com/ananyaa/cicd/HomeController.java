package com.ananyaa.cicd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    // CHANGE THIS LINE for the redeployment demo (e.g. "v1" -> "v2")
    static final String MESSAGE = "Hello from the CI/CD pipeline - v1";

    @Value("${IMAGE_TAG:local}")
    private String imageTag;

    @Value("${HOSTNAME:localhost}")
    private String podName;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <html><head><title>CI/CD Demo</title></head>
                <body style="font-family:sans-serif;max-width:640px;margin:60px auto">
                  <h1>%s</h1>
                  <p>Image tag (git commit): <b>%s</b></p>
                  <p>Served by pod: <b>%s</b></p>
                  <p>Pipeline: GitHub &rarr; Maven &rarr; Docker &rarr; Azure Container Registry &rarr; AKS (via Ansible)</p>
                </body></html>
                """.formatted(MESSAGE, imageTag, podName);
    }

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of("message", MESSAGE, "imageTag", imageTag, "pod", podName);
    }
}
