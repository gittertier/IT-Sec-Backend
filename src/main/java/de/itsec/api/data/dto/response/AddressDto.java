package de.itsec.api.data.dto.response;

import de.itsec.api.data.Address;

public record AddressDto(String street, String houseNumber, String postalCode, String city) {

  public static AddressDto from(Address address) {
    return new AddressDto(
        address.getStreet(), address.getHouseNumber(), address.getAreaCode(), address.getCity());
  }
}
