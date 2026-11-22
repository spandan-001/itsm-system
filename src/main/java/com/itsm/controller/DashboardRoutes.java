package com.itsm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardRoutes {
    @GetMapping({"/employee", "/technician", "/admin", "/my-incidents",
            "/report-incident", "/notifications", "/incidents/{id:[0-9]+}"})
    public String dashboard() {
        return "forward:/index.html";
    }
}
