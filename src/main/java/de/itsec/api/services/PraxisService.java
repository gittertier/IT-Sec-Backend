package de.itsec.api.services;

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

  /** Creates and persists a new praxis. */
  public Praxis create(String name, String address, String postalCode) {
    Praxis praxis = new Praxis();
    praxis.setName(name);
    praxis.setAddress(address);
    praxis.setPostalCode(postalCode);
    return praxisRepository.save(praxis);
  }

  /**
   * Lists praxen, optionally filtered by postal code and/or a name fragment. A {@code null} or blank
   * filter value is ignored.
   */
  public Page<Praxis> find(String postalCode, String name, Pageable pageable) {
    boolean hasPostalCode = postalCode != null && !postalCode.isBlank();
    boolean hasName = name != null && !name.isBlank();

    if (hasPostalCode && hasName) {
      return praxisRepository.findByPostalCodeAndNameContainingIgnoreCase(
          postalCode, name, pageable);
    }
    if (hasPostalCode) {
      return praxisRepository.findByPostalCode(postalCode, pageable);
    }
    if (hasName) {
      return praxisRepository.findByNameContainingIgnoreCase(name, pageable);
    }
    return praxisRepository.findAll(pageable);
  }
}
