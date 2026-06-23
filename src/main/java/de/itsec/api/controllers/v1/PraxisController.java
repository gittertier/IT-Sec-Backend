package de.itsec.api.controllers.v1;

import de.itsec.api.data.dto.request.PraxisCreateRequestDto;
import de.itsec.api.data.dto.response.PraxisDto;
import de.itsec.api.data.termin.Praxis;
import de.itsec.api.services.PraxisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for listing and (for admins/staff) creating medical practices. */
@RestController
@RequestMapping("/api/v1/praxen")
public class PraxisController {

  private final PraxisService praxisService;

  @Autowired
  public PraxisController(PraxisService praxisService) {
    this.praxisService = praxisService;
  }

  /** Lists praxen, optionally filtered by postal code (PLZ) and/or a name fragment. */
  @GetMapping
  public List<PraxisDto> list(
      @RequestParam(required = false) String postalCode,
      @RequestParam(required = false) String name) {
    return praxisService.find(postalCode, name).stream().map(PraxisDto::from).toList();
  }

  /** Creates a new praxis. Restricted to admin/staff. */
  @PostMapping
  @Secured({"ROLE_ADMIN", "ROLE_STAFF"})
  public ResponseEntity<PraxisDto> create(@Valid @RequestBody PraxisCreateRequestDto request) {
    Praxis praxis = praxisService.create(request.name(), request.address(), request.postalCode());
    return new ResponseEntity<>(PraxisDto.from(praxis), HttpStatus.CREATED);
  }
}
