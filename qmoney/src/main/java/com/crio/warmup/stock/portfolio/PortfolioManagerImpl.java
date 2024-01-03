
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {




  private RestTemplate restTemplate;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  private StockQuotesService stockQuotesService;
  PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  } 

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  // public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
  //     throws JsonProcessingException {
  //       /*get stocks from the startDate to endDate
  //       startDate-purchaseDate
  //       throw error if the startdate is not before the endDate
  //       */
  //       if(from.compareTo(to)>=0){
  //         throw new RuntimeException();
  //       }
  //       //create a url object for the api call
  //       String url=buildUri(symbol, from, to);
  //       //api returns a list of results for each day's stock data
  //       TiingoCandle[] stocksStartToEndDate=restTemplate.getForObject(url,TiingoCandle[].class);
  //       if(stocksStartToEndDate==null){
  //         return new ArrayList<Candle>();
  //       }
  //       else{
  //         List<Candle> stock=Arrays.asList(stocksStartToEndDate);
  //         return stock;
  //       }
  //       // List<Candle> stocksList=Arrays.asList(stocksStartToEndDate);
  //       // return stocksList;
  // }

  
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
      String token="fec086d1f152dd00954d0682f8005050e9c650aa";
      String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
      String url=uriTemplate.replace("$APIKEY",token).replace("$SYMBOL", symbol).replace("$STARTDATE", startDate.toString()).replace("$ENDDATE", endDate.toString());

      return url;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns=new ArrayList<AnnualizedReturn>();

    //loop through each of the portfolio trade objects
    for(int i=0;i<portfolioTrades.size();i++){
      //get annualized return objects for each
      annualizedReturn= getAnnualizedReturn(portfolioTrades.get(i),endDate);
      //add those as list
      annualizedReturns.add(annualizedReturn);
    }
    Comparator<AnnualizedReturn> SortByAnnReturn=Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Collections.sort(annualizedReturns,SortByAnnReturn);

    return annualizedReturns;
  }


  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate) throws StockQuoteServiceException {
    
    
    LocalDate startDate=trade.getPurchaseDate();
    
    String symbol=trade.getSymbol();

    Double buyPrice = 0.0, sellPrice = 0.0;

    try{

      LocalDate startLocalDate=trade.getPurchaseDate();
      //Fetch data
      List<Candle> stocksStartToEndFull = getStockQuote(symbol, startLocalDate, endDate);

      Collections.sort(stocksStartToEndFull, (candle1, candle2) -> {
        return candle1.getDate().compareTo(candle2.getDate());
      });

      //Extract stocks for startDate & endDate
      Candle stockStartDate = stocksStartToEndFull.get(0);
      Candle stockLatest=stocksStartToEndFull.get(stocksStartToEndFull.size()-1);

       buyPrice=stockStartDate.getOpen();
       sellPrice=stockLatest.getClose();
       endDate=stockLatest.getDate();
    
      
    } catch(JsonProcessingException e){
      throw new RuntimeException();
      } 
       //calcuate total returns
      Double totalReturn=(sellPrice-buyPrice)/buyPrice;

      long  daysBetweenPurchaseAndSelling = ChronoUnit.DAYS.between(startDate, endDate);
      //calculate years
      Double totalYears=(double) (daysBetweenPurchaseAndSelling)/365;
      //calculate annualized returns using formula
      Double annualizedReturn= Math.pow((1+totalReturn),(1/totalYears))-1;

      return new AnnualizedReturn(symbol, annualizedReturn, totalReturn);
  }

  


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

  public List<Candle> getStockQuote(String symbol, LocalDate startLocalDate, LocalDate endLocalDate) throws JsonProcessingException, StockQuoteServiceException{
    return stockQuotesService.getStockQuote(symbol, startLocalDate, endLocalDate);

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws StockQuoteServiceException, InterruptedException {

        List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
        List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < portfolioTrades.size(); i++) {
          PortfolioTrade trade = portfolioTrades.get(i);
          Callable<AnnualizedReturn> callableTask = () -> {
            return getAnnualizedReturn(trade, endDate);
          };
          Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
          futureReturnsList.add(futureReturns);
        }
      
        for (int i = 0; i < portfolioTrades.size(); i++) {
          Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
          try {
            AnnualizedReturn returns = futureReturns.get();
            annualizedReturns.add(returns);
          } catch (ExecutionException e) {
            throw new StockQuoteServiceException("Error when calling the API", e);
      
          }
        }
        Collections.sort(annualizedReturns, Collections.reverseOrder());
        return annualizedReturns;
      }
      
    
}

