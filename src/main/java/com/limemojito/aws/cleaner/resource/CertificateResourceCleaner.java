/*
 * Copyright 2020 Lime Mojito Pty Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.certificatemanager.AWSCertificateManager;
import com.amazonaws.services.certificatemanager.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.limemojito.aws.cleaner.resource.Throttle.performWithThrottle;
import static java.util.stream.Collectors.toList;

@Service
public class CertificateResourceCleaner extends PhysicalResourceCleaner {
    private final AWSCertificateManager certificateManager;

    @Autowired
    public CertificateResourceCleaner(AWSCertificateManager certificateManager) {
        super();
        this.certificateManager = certificateManager;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        final ListCertificatesResult result = certificateManager.listCertificates(new ListCertificatesRequest());
        return result.getCertificateSummaryList()
                     .stream()
                     .map(CertificateSummary::getCertificateArn)
                     .map(this::fetchCertificateDetails)
                     .filter(d -> d.getInUseBy().isEmpty())
                     .map(CertificateDetail::getCertificateArn)
                     .collect(toList());
    }

    private CertificateDetail fetchCertificateDetails(String arn) {
        final List<CertificateDetail> result = new ArrayList<>();
        performWithThrottle(() -> {
            final CertificateDetail detail = certificateManager.describeCertificate(new DescribeCertificateRequest().withCertificateArn(arn))
                                                               .getCertificate();
            result.add(detail);
        });
        return result.get(0);
    }

    @Override
    protected void performDelete(String physicalId) {
        performWithThrottle(() -> certificateManager.deleteCertificate(new DeleteCertificateRequest().withCertificateArn(physicalId)));
    }
}
