package com.ercanbeyen.bankingapplication.job;

import com.ercanbeyen.bankingapplication.entity.BankNews;
import com.ercanbeyen.bankingapplication.entity.NewsReport;
import com.ercanbeyen.bankingapplication.entity.OfferNews;
import com.ercanbeyen.bankingapplication.repository.BankNewsRepository;
import com.ercanbeyen.bankingapplication.repository.NewsReportRepository;
import com.ercanbeyen.bankingapplication.repository.OfferNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Writer {
    private static final String SAVE_METHOD = "save";
    private final NewsReportRepository newsReportRepository;
    private final BankNewsRepository bankNewsRepository;
    private final OfferNewsRepository offerNewsRepository;

    @Bean(name = "writerNewsReportTable")
    public RepositoryItemWriter<NewsReport> writerNewsReportTable() {
        RepositoryItemWriter<NewsReport> writer = new RepositoryItemWriter<>(newsReportRepository);
        writer.setMethodName(SAVE_METHOD);
        return writer;
    }

    @Bean(name = "writerBankNewsTable")
    public RepositoryItemWriter<BankNews> writerBankNewsTable() {
        RepositoryItemWriter<BankNews> writer = new RepositoryItemWriter<>(bankNewsRepository);
        writer.setMethodName(SAVE_METHOD);
        return writer;
    }

    @Bean(name = "writerOfferNewsTable")
    public RepositoryItemWriter<OfferNews> writerOfferNewsTable() {
        RepositoryItemWriter<OfferNews> writer = new RepositoryItemWriter<>(offerNewsRepository);
        writer.setMethodName(SAVE_METHOD);
        return writer;
    }
}
