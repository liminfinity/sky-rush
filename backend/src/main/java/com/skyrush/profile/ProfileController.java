package com.skyrush.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile and collection")
public class ProfileController {
  public record EquipRequest(@NotBlank @Size(max = 32) String cosmeticId) {}

  private final ProfileService profiles;
  private final CollectionService collection;

  public ProfileController(ProfileService profiles, CollectionService collection) {
    this.profiles = profiles;
    this.collection = collection;
  }

  @GetMapping
  @Operation(summary = "Own profile and lifetime records from completed rounds")
  public ProfileService.Profile profile() {
    return profiles.current();
  }

  @GetMapping("/collection")
  @Operation(summary = "Own earned collection and equipped cosmetics")
  public CollectionService.Collection collection() {
    return collection.current();
  }

  @PostMapping("/cosmetics/equip")
  @Operation(
      summary = "Equip an unlocked cosmetic",
      description =
          "Example: {\"cosmeticId\":\"constellations\"}. Session user only; CSRF required. No gameplay effect.")
  public CollectionService.Collection equip(@Valid @RequestBody EquipRequest request) {
    return collection.equip(request.cosmeticId());
  }
}
