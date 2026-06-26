package de.itsec.api.services;

import de.itsec.api.data.Address;
import de.itsec.api.data.dto.response.AddressDto;
import de.itsec.api.data.dto.response.CoordinatesDto;
import de.itsec.api.data.termin.Praxis;
import de.itsec.api.repositories.termin.PraxisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Management and lookup of medical practices (Praxen). */
@Service
public class PraxisService {

  private final PraxisRepository praxisRepository;

  @Autowired
  public PraxisService(PraxisRepository praxisRepository) {
    this.praxisRepository = praxisRepository;
  }

  /** Creates and persists a new praxis from a structured address and its map coordinates. */
  public Praxis create(String name, AddressDto address, CoordinatesDto coordinates) {
    Praxis praxis = new Praxis();
    praxis.setName(name);
    praxis.setAddress(toAddress(address));
    praxis.setLatitude(coordinates.lat());
    praxis.setLongitude(coordinates.lon());
    return praxisRepository.save(praxis);
  }

  /**
   * Lists praxen, optionally filtered by postal code and/or a name fragment. A {@code null} or blank
   * filter value is ignored. The PLZ now lives in the address, so we filter on the address areaCode.
   */
  public Page<Praxis> find(String postalCode, String name, Pageable pageable) {
    boolean hasPostalCode = postalCode != null && !postalCode.isBlank();
    boolean hasName = name != null && !name.isBlank();

    if (hasPostalCode && hasName) {
      return praxisRepository.findByAddress_AreaCodeAndNameContainingIgnoreCase(
          postalCode, name, pageable);
    }
    if (hasPostalCode) {
      return praxisRepository.findByAddress_AreaCode(postalCode, pageable);
    }
    if (hasName) {
      return praxisRepository.findByNameContainingIgnoreCase(name, pageable);
    }
    return praxisRepository.findAll(pageable);
  }

  private Address toAddress(AddressDto dto) {
    return Address.builder()
        .street(dto.street())
        .houseNumber(dto.houseNumber())
        .areaCode(dto.postalCode())
        .city(dto.city())
        .build();
  }
}
