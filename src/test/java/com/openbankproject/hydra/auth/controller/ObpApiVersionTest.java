package com.openbankproject.hydra.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every OBP-API URL Hola calls uses API version v5.1.0 or newer, so a deployment can
 * disable the older versions without breaking Hola.
 */
class ObpApiVersionTest {

    private static final Pattern OBP_VERSION = Pattern.compile("/obp/v(\\d+)\\.(\\d+)\\.(\\d+)/");
    private static final int[] MINIMUM = {5, 1, 0};

    private static List<String> declaredObpUrls(Class<?> controller) {
        List<String> urls = new ArrayList<>();
        for (Field field : controller.getDeclaredFields()) {
            Value value = field.getAnnotation(Value.class);
            if (value != null && value.value().contains("/obp/v")) {
                urls.add(field.getName() + " = " + value.value());
            }
        }
        return urls;
    }

    private static boolean olderThanMinimum(String url) {
        Matcher m = OBP_VERSION.matcher(url);
        while (m.find()) {
            for (int i = 0; i < 3; i++) {
                int part = Integer.parseInt(m.group(i + 1));
                if (part != MINIMUM[i]) {
                    if (part < MINIMUM[i]) return true;
                    break;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("controllers declare OBP-API URLs on v5.1.0 or newer")
    void controllersUseV510OrNewer() {
        List<String> urls = new ArrayList<>();
        urls.addAll(declaredObpUrls(IndexController.class));
        urls.addAll(declaredObpUrls(OtherController.class));

        assertThat(urls).as("OBP-API URLs found").isNotEmpty();
        assertThat(urls).filteredOn(ObpApiVersionTest::olderThanMinimum).isEmpty();
    }
}
