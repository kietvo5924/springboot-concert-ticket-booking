package com.geekup.ticketbooking.controller;

import com.geekup.ticketbooking.dto.ConcertResponseDto;
import com.geekup.ticketbooking.dto.TicketCategoryResponseDto;
import com.geekup.ticketbooking.entity.Concert;
import com.geekup.ticketbooking.entity.TicketCategory;
import com.geekup.ticketbooking.repository.ConcertRepository;
import com.geekup.ticketbooking.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    @GetMapping
    public ResponseEntity<List<ConcertResponseDto>> getAllConcerts() {
        List<ConcertResponseDto> concerts = concertRepository.findAll().stream()
                .map(this::mapToConcertResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(concerts);
    }

    @GetMapping("/{id}/categories")
    public ResponseEntity<List<TicketCategoryResponseDto>> getTicketCategories(@PathVariable Long id) {
        List<TicketCategoryResponseDto> categories = ticketCategoryRepository.findByConcertId(id).stream()
                .map(this::mapToTicketCategoryResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    private ConcertResponseDto mapToConcertResponseDto(Concert concert) {
        return ConcertResponseDto.builder()
                .id(concert.getId())
                .name(concert.getName())
                .description(concert.getDescription())
                .startTime(concert.getStartTime())
                .endTime(concert.getEndTime())
                .location(concert.getLocation())
                .build();
    }

    private TicketCategoryResponseDto mapToTicketCategoryResponseDto(TicketCategory category) {
        return TicketCategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .price(category.getPrice())
                .initialQuantity(category.getInitialQuantity())
                .remainingQuantity(category.getRemainingQuantity())
                .build();
    }
}
