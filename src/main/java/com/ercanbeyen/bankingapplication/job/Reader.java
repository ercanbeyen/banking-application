package com.ercanbeyen.bankingapplication.job;

import com.ercanbeyen.bankingapplication.constant.enums.NewsType;
import com.ercanbeyen.bankingapplication.entity.NewsReport;
import com.ercanbeyen.bankingapplication.repository.NewsReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Reader {
    private static final String FIND_BY_TYPE_METHOD = "findByType";
    @Value("${dataset.news.path}")
    private String newsPath;

    private final NewsReportRepository newsReportRepository;

    @Bean(name = "readerNewsReportCSVFile")
    public ItemReader<NewsReport> readerNewsReportCSVFile() {
        return new FlatFileItemReaderBuilder<NewsReport>()
                .name("readerNewsReportCSVFile")
                .resource(new ClassPathResource(newsPath))
                .linesToSkip(1)
                .delimited()
                .names("title", "url", "type")
                .targetType(NewsReport.class)
                .build();
    }

    @Bean(name = "readerNewsReportByBankNewsType")
    public ItemReader<NewsReport> readerNewsReportByBankNewsType() {
        return getNewsReportRepositoryItemReader(NewsType.BANK_NEWS);
    }

    @Bean(name = "readerNewsReportByOfferNewsType")
    public ItemReader<NewsReport> readerNewsReportByOfferNewsType() {
        return getNewsReportRepositoryItemReader(NewsType.OFFER_NEWS);
    }

    private RepositoryItemReader<NewsReport> getNewsReportRepositoryItemReader(NewsType newsType) {
        Map<String, Sort.Direction> sorts = new LinkedHashMap<>();
        sorts.put("id", Sort.Direction.ASC);

        RepositoryItemReader<NewsReport> reader = new RepositoryItemReader<>(newsReportRepository, sorts);
        reader.setRepository(newsReportRepository);
        reader.setMethodName(FIND_BY_TYPE_METHOD);

        List<Object> queryMethodArguments = new ArrayList<>();
        queryMethodArguments.add(newsType);
        reader.setArguments(queryMethodArguments);

        return reader;
    }

}
