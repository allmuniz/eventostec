package com.project.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.project.api.domain.coupon.Coupon;
import com.project.api.domain.event.Event;
import com.project.api.domain.event.EventDetailsDTO;
import com.project.api.domain.event.EventRequestDTO;
import com.project.api.domain.event.EventResponseDTO;
import com.project.api.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    private final AmazonS3 s3client;

    private final EventRepository eventRepository;

    private final AddressService addressService;

    private final CouponService couponService;

    public EventService(AmazonS3 s3client, EventRepository eventRepository, AddressService addressService, CouponService couponService) {
        this.s3client = s3client;
        this.eventRepository = eventRepository;
        this.addressService = addressService;
        this.couponService = couponService;
    }

    public Event createEvent(EventRequestDTO data) {
        String imgUrl = null;

        if (data.image() != null){
            imgUrl = this.uploadImg(data.image());
        }

        Event newEvent = new Event();
        newEvent.setTitle(data.title());
        newEvent.setDescription(data.description());
        newEvent.setEventUrl(data.eventUrl());
        newEvent.setImgUrl(imgUrl);
        newEvent.setDate(new Date(data.date()));
        newEvent.setRemote(data.remote());

        eventRepository.save(newEvent);

        if (!data.remote()){
            this.addressService.createAddress(data, newEvent);
        }

        return newEvent;
    }

    public List<EventResponseDTO> getUpcomingEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventsPage = eventRepository.findUpcomingEvents(new Date(), pageable);
        return eventsPage.map(event -> new EventResponseDTO(event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getDate(),
                        event.getAddress() != null ? event.getAddress().getCity() : "",
                        event.getAddress() != null ? event.getAddress().getUf() : "",
                        event.getRemote(),
                        event.getEventUrl(),
                        event.getImgUrl()))
                .stream().toList();

    }

    public List<EventResponseDTO> getFilteredEvents(int page, int size, String title,String city, String uf, Date startDate, Date endDate) {

        title = (title != null) ? title : "";
        city = (city != null) ? city : "";
        uf = (uf != null) ? uf : "";
        startDate = (startDate != null) ? startDate : new Date();
        endDate = (endDate != null) ? endDate : Date.from(LocalDate.now().plusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Pageable pageable = PageRequest.of(page, size);

        Page<Event> eventsPage = eventRepository.findFilteredEvents(title, city, uf, startDate, endDate, pageable);
        return eventsPage.map(event -> new EventResponseDTO(event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getDate(),
                        event.getAddress() != null ? event.getAddress().getCity() : "",
                        event.getAddress() != null ? event.getAddress().getUf() : "",
                        event.getRemote(),
                        event.getEventUrl(),
                        event.getImgUrl()))
                .stream().toList();

    }

    public EventDetailsDTO getEventDetails(UUID eventId){
        Event event = this.eventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));

        List<Coupon> coupons = this.couponService.consultCoupons(eventId, new Date());

        List<EventDetailsDTO.CouponDTO> couponDTOs = coupons.stream()
                .map(coupon -> new EventDetailsDTO.CouponDTO(
                        coupon.getCode(),
                        coupon.getDiscount(),
                        coupon.getValid()))
                .collect(Collectors.toList());

        return new EventDetailsDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getAddress() != null ? event.getAddress().getCity() : "",
                event.getAddress() != null ? event.getAddress().getUf() : "",
                event.getImgUrl(),
                event.getEventUrl(),
                couponDTOs);
    }

    private String uploadImg(MultipartFile multipartFile) {
        String fileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

        try {
            File file = this.convertMultipartToFile(multipartFile);
            s3client.putObject(bucketName, fileName, file);

            file.delete();
            return s3client.getUrl(bucketName, fileName).toString();
        } catch (Exception e){
            System.out.println("error ao subir file");
            return null;
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws Exception{
        File convFile = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipartFile.getBytes());
        fos.close();
        return convFile;
    }
}
