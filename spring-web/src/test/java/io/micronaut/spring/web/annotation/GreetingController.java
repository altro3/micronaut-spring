/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.spring.web.annotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.validation.Validated;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Pattern;
import reactor.core.publisher.Flux;

import static org.junit.Assert.assertEquals;

@RestController
@Validated
public class GreetingController {

    private static final String TEMPLATE = "Hello, %s!";

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Welcome to Micronaut for Spring!");
        return "welcome";
    }

    @PostMapping("/request")
    public Flux<String> request(ServerHttpRequest request, HttpMethod method) {
        assertEquals("/request", request.getPath().value());
        assertEquals(HttpMethod.POST, request.getMethod());
        assertEquals(HttpMethod.POST, method);
        assertEquals("Bar", request.getHeaders().getFirst("Foo"));
        return request.getBody().map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") @Pattern(regexp = "\\D+") String name) {
        return new Greeting(counter.incrementAndGet(), TEMPLATE.formatted(name));
    }

    @PostMapping("/greeting")
    public Greeting greetingByPost(@RequestBody Greeting greeting) {
        return new Greeting(counter.incrementAndGet(), TEMPLATE.formatted(greeting.getContent()));
    }

    @DeleteMapping("/greeting")
    public ResponseEntity<?> deleteGreeting() {
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("Foo", "Bar");
        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    }

    @RequestMapping("/greeting-status")
    @ResponseStatus(code = HttpStatus.CREATED)
    public Greeting greetingWithStatus(@RequestParam(value = "name", defaultValue = "World") @Pattern(regexp = "\\D+") String name) {
        return new Greeting(counter.incrementAndGet(), TEMPLATE.formatted(name));
    }

    @PostMapping(value = "/multipart-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String multipartRequest(@RequestPart String json,
                                   @RequestPart("myFile") CompletedFileUpload file) throws IOException {

        return json + '#' + new String(file.getInputStream().readAllBytes());
    }

}
