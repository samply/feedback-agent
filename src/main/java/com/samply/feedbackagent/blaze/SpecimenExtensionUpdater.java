package com.samply.feedbackagent.blaze;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;

import java.util.List;
public class SpecimenExtensionUpdater {

    private final FhirContext fhirContext;
    private final String serverBase;

    public SpecimenExtensionUpdater(String serverBase) {
        this.fhirContext = FhirContext.forR4();
        this.serverBase = serverBase;
    }

    public void updateSpecimenWithExtension(List<String> specimenIds, String publicationRefUrl) {
        // Create the batch URL by concatenating the specimen IDs
        String batchUrl = "Specimen?_id=" + String.join(",", specimenIds);

        // Perform HTTP request to retrieve the specimen resources from blaze
        Bundle bundle = fhirContext.newRestfulGenericClient(serverBase)
                .search()
                .byUrl(batchUrl)
                .returnBundle(Bundle.class)
                .execute();

        Extension extension = new Extension();
        extension.setUrl("PublicationReference");
        extension.setValue(new StringType(publicationRefUrl));

        // Iterate over the resources in the Bundle
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Specimen specimen) {
                specimen.addExtension(extension);
            }
        }
        // Create a new Bundle to contain the updated specimens
        Bundle updatedBundle = new Bundle();
        updatedBundle.setType(Bundle.BundleType.TRANSACTION);

        // Add each updated specimen as an entry in the Bundle
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            String resourceType = entry.getResource().fhirType();
            String resourceId = entry.getResource().getIdElement().getIdPart();
            String url = resourceType + "/" + resourceId;

            updatedBundle.addEntry()
                    .setFullUrl(entry.getFullUrl())
                    .setResource(entry.getResource())
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl(url);
        }
        // Perform HTTP request to save the updated Bundle to the server
        IGenericClient client = fhirContext.newRestfulGenericClient(serverBase);
        client.transaction()
                .withBundle(updatedBundle)
                .execute();
    }
}
