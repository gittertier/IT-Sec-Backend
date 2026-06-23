package de.itsec.api.data.dto.response;

import de.itsec.api.data.termin.Praxis;
import java.util.UUID;

/** Public view of a medical practice, used for praxis lists and selection dropdowns. */
public record PraxisDto(UUID id, String name, String address, String postalCode) {

  public static PraxisDto from(Praxis praxis) {
    return new PraxisDto(
        praxis.getId(), praxis.getName(), praxis.getAddress(), praxis.getPostalCode());
  }
}
