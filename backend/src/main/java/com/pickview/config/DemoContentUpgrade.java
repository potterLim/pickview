package com.pickview.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickview.domain.EAccessTerm;
import com.pickview.domain.WonAmount;
import com.pickview.model.Product;
import com.pickview.repository.IProductRepository;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class DemoContentUpgrade implements CommandLineRunner {

    private final IProductRepository mProducts;
    private final ObjectMapper mMapper;
    private final boolean mIsEnabled;

    public DemoContentUpgrade(
        IProductRepository products,
        ObjectMapper mapper,
        @Value("${pickview.seed}") boolean isEnabled
    ) {
        mProducts = products;
        mMapper = mapper;
        mIsEnabled = isEnabled;
    }

    @Override
    @Transactional
    public void run(String... arguments) throws Exception {
        if (!mIsEnabled) {
            return;
        }
        try (InputStream source = new ClassPathResource("demo-catalog.json").getInputStream()) {
            for (JsonNode content : mMapper.readTree(source)) {
                mProducts
                    .findById(content.path("id").asText())
                    .ifPresent(product -> upgradeOriginalSample(product, content));
            }
        }
    }

    private void upgradeOriginalSample(Product product, JsonNode content) {
        // Never overwrite user uploads or repeat the content migration on later starts.
        if (!product.getMediaKey().equals("demo.mp4")) {
            return;
        }
        com.pickview.domain.EProductStatus status = product.getStatus();
        product.revise(
            content.path("title").asText(),
            content.path("description").asText(),
            new WonAmount(product.getPriceWon()),
            EAccessTerm.parseDays(product.getTermDays())
        );
        product.changePresentation(com.pickview.domain.ECategory.valueOf(content.path("category").asText()), content.path("thumbnail").asText());
        product.changeTags(content.path("tags").asText());
        product.replaceMedia(
            "sample-" + product.getId() + ".mp4",
            "sample-" + product.getId() + "-preview.mp4",
            content.path("duration").asDouble()
        );
        switch (status) {
            case APPROVED -> product.publish();
            case WITHDRAWN -> product.withdraw();
            case REJECTED -> product.reject();
            default -> product.submit();
        }
    }
}
