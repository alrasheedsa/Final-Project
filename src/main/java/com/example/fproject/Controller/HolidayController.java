package com.example.fproject.Controller;

import com.example.fproject.Service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping("/public/{year}/{countryCode}")
    public ResponseEntity<?> getPublicHolidays(@PathVariable Integer year,
                                               @PathVariable String countryCode) {
        return ResponseEntity.status(200).body(holidayService.getPublicHolidays(year, countryCode));
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkHoliday(@RequestParam String date,
                                          @RequestParam(defaultValue = "SA") String countryCode) {
        return ResponseEntity.status(200).body(holidayService.checkHoliday(date, countryCode));
    }
}
