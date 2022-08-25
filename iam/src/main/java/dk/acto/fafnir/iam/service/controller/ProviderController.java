package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.exception.InvalidConfiguration;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.ProviderService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("/iam/org/{orgId}/pro")
@PreAuthorize("hasAuthority(T(dk.acto.fafnir.iam.security.IAMRoles).FAFNIR_ADMIN.toString())")
public class ProviderController {

    private final AdministrationService administrationService;
    private final ProviderService providerService;
    private final DtoFactory dtoFactory;

    @GetMapping
    public ModelAndView getProviderPicker(@PathVariable String orgId) {
        var organisation = administrationService.readOrganisation(orgId);
        var model = Map.of(
                "providers", MetadataProvider.getAllSupportedProviders(),
                "organisation", organisation
        );
        return new ModelAndView("provider_picker", model);
    }

    @PostMapping()
    public ModelAndView getProviderConfig(@PathVariable String orgId, @RequestParam String providerId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("providerId", providerId);
        return new ModelAndView("redirect:/iam/org/" + orgId + "/pro/conf");
    }

    @GetMapping("/conf")
    public ModelAndView editProviderConfig(@PathVariable String orgId, @RequestParam(required = false) Optional<String> providerId){
        var organisation = administrationService.readOrganisation(orgId);
        var providerConf = providerId.map(providerService::getProviderMetaData)
                .map(MetadataProvider::empty)
                .or(() -> Optional.ofNullable(organisation.getProviderConfiguration()))
                .orElseThrow(InvalidConfiguration::new);
        var providerMetadata = providerService.getProviderMetaData(providerConf.getProviderId());
        var model = Map.of(
                "provider", providerMetadata,
                "supportsClaims", providerService.supportsClaims(providerMetadata.getProviderId()),
                "providerConf", providerConf.getValues().entrySet(),
                "organisation", organisation
        );
        return new ModelAndView("provider_conf", model);
    }

    @PostMapping("/conf")
    public ModelAndView confirmProviderConfig(@PathVariable String orgId, @RequestParam Map<String, String> providerDataMap){
        var org = administrationService.readOrganisation(orgId);
        var providerConf = dtoFactory.fromMap(providerDataMap);
        org = org.toBuilder()
                .providerConfiguration(providerConf)
                .build();
        administrationService.updateOrganisation(org);
        return new ModelAndView("redirect:/iam/org/page/1");
    }

}
