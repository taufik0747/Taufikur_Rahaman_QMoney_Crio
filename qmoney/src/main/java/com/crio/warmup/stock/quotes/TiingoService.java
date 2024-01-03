
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

public static final String TOKEN="fec086d1f152dd00954d0682f8005050e9c650aa";

  private RestTemplate restTemplate;
  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
        /*get stocks from the startDate to endDate
        startDate-purchaseDate
        throw error if the startdate is not before the endDate
        */
        List<Candle> stocksStartToEndDate = new ArrayList<>();

        if(from.compareTo(to)>=0){
          throw new RuntimeException();
        }
        //create a url object for the api call
        String url=buildUri(symbol, from, to);

    try{
        String stocks = restTemplate .getForObject(url, String.class);
        ObjectMapper objectMapper = getObjectMapper();
        //api returns a list of results for each day's stock data
        TiingoCandle[] stocksStartToEndDateArray=objectMapper.readValue(stocks,TiingoCandle[].class);
        
        stocksStartToEndDate = Arrays.asList(stocksStartToEndDateArray); 
        
        } catch(NullPointerException e) {
          throw new StockQuoteServiceException("Error occured when requesting response from Tiingo API", e.getCause());
        }
        // List<Candle> stocksList=Arrays.asList(stocksStartToEndDate);
         return stocksStartToEndDate;
  }


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

    String uriTemplate = String.format("https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?" + "startDate=%s&endDate=%s&token=%s", symbol, startDate,endDate,TOKEN);

    return uriTemplate;
  }





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF


}
