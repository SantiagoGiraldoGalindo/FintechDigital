package com.fintech.backend.controller;

import com.fintech.backend.service.benchmark.BenchmarkService;
import com.fintech.backend.service.benchmark.BenchmarkService.BenchmarkReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint que permite al frontend solicitar una comparación de estructuras. */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    public BenchmarkReport ejecutar() {
        return benchmarkService.ejecutar();
    }
}
