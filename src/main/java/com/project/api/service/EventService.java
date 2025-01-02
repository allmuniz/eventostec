package com.project.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.project.api.domain.event.Event;
import com.project.api.domain.event.EventRequestDTO;
import com.project.api.domain.event.EventResponseDTO;
import com.project.api.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class EventService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3client;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AddressService addressService;

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

    private String uploadImg(MultipartFile multipartFile) {
        String fileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

        try {
            File file = this.convertMultipartToFile(multipartFile);
            s3client.putObject(bucketName, fileName, file);

            file.delete();
            return s3client.getUrl(bucketName, fileName).toString();
        } catch (Exception e){
            System.out.println("erro ao subir arquivo");
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
