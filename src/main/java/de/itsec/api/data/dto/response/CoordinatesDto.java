package de.itsec.api.data.dto.response;

/** A geographic point (latitude/longitude), used to place a praxis on a map. */
public record CoordinatesDto(double lat, double lon) {

  // Returns null when the praxis has no coordinates yet, so the field stays empty
  // instead of defaulting to 0/0 (which would point into the ocean off Africa).
  public static CoordinatesDto from(Double lat, Double lon) {
    if (lat == null || lon == null) {
      return null;
    }
    return new CoordinatesDto(lat, lon);
  }
}
