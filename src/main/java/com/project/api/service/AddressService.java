package com.project.api.service;

import com.project.api.domain.address.Address;
import com.project.api.domain.event.Event;
import com.project.api.domain.event.EventRequestDTO;
import com.project.api.repositories.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    public Address createAddress(EventRequestDTO data, Event event) {
        Address address = new Address();
        address.setCity(data.city());
        address.setUf(data.uf());
        address.setEvent(event);

        return addressRepository.save(address);
    }
}
