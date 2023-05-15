package application;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


//클라이언트
public class Main extends Application {

   Socket socket;
   TextArea textArea;
   TextArea namelist;
   TextField userName; //<----------------start() 메서드 안에 있던거 전역으로 바꿨음
   TextField whisperTarget;      //<----------추가
   
   Integer count = 0;
   TextField notice;
   HashSet<String> clientmap = new HashSet<String>();
   Button connectionButton = new Button("접속하기");
  
   Stage window;
   Scene scene2;
   
   TextArea textArea2;
   
   // 클라이언트 프로그램 동작 메소드 (어떤 IP로 , 어떤 port로 접속할지 정해줌)
   public void startClient(String IP, int port) {
      // 스레드 객체 생성!
      Thread thread = new Thread() {
         public void run() {
            try {
               // socket 초기화
               socket = new Socket(IP, port);
               receive();
            } catch (Exception e) {
               // 오류가 생긴다면
               if (!socket.isClosed()) {
                  stopClient();

                  System.out.println("[서버 접속 실패]");
                  Platform.exit();
               }
            }
         }
      };
      thread.start();
   }

   // 클라이언트 프로그램 종료 메소드
   public void stopClient() {
      try {
         if(socket != null && !socket.isClosed()) {
            socket.close(); 
         }
      }catch(Exception e){
         e.printStackTrace();
      }
   }

   // 서버로부터 메시지를 전달받는 메소드
   public void receive() {
      // 서버 프로그램으로부터 메시지를 계속 전달 받을 수 있도록
      while (true) {
         try {
            // 서버로부터 메시지를 전달 받을 수 있도록
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[512];
            int length = in.read(buffer);
            if (length == -1)
               throw new IOException();
            String message = new String(buffer, 0, length, "UTF-8");
            
            //====================================수정한 부분=========================================
            //#으로 String 잘라서 배열에 담기, 0번 인덱스: 메시지 내용(날짜) / 1번 인덱스: 메시지 보낸사람 / 2번 인덱스: 귓속말 받는사람
            String[] str = message.split("#");      
            String msg = filtering(str[0]);
         
            // 전체 채팅
            if(str[2] == "") {
               Platform.runLater(() -> {
                  // textArea는 GUI요소 중 하나로 화면에 어떠한 메시지를 주고 받았는지 출력해 주는 요소.
                  textArea.appendText(msg);
                  textArea.appendText("\n");
                  namelist.appendText(str[1] + "\n");
               });
            }
            
            // 귓속말 채팅
            else if(str[1].equals(userName.getText())) {
               Platform.runLater(() -> {
                  // textArea는 GUI요소 중 하나로 화면에 어떠한 메시지를 주고 받았는지 출력해 주는 요소.
                  textArea.appendText("(귓속말 : ");
                  textArea.appendText(str[1] + "가 " + str[2] +"에게)  ");
                  textArea.appendText(msg);
                  textArea.appendText("\n");
               });
            } else if(str[2].equals(userName.getText())) {
               Platform.runLater(() -> {
                  // textArea는 GUI요소 중 하나로 화면에 어떠한 메시지를 주고 받았는지 출력해 주는 요소.
                  textArea.appendText("(귓속말) ");
                  textArea.appendText(msg);
                  textArea.appendText("\n");
               });
            }
            //=====================================================================================
            
         } catch (Exception e) {
            stopClient();
            break;
         }
      }
   }

   // 서버로 메시지를 전송하는 메소드
   public void send(String message) {
      Thread thread = new Thread() {
         public void run() {
            try {
               OutputStream out = socket.getOutputStream();
               
               //메시지내용(날짜) # 메시지 보낸사람 # 귓속말 받는사람
               String formatMessage = addDateFormat(message) + "#" + userName.getText()
               + "#" + whisperTarget.getText() + "#" + "\n";      
               byte[] buffer = formatMessage.getBytes("UTF-8");
               out.write(buffer);
               out.flush();
            } catch (Exception e) {
               stopClient();
            }
         }
         //========================메시지 뒤에 날짜 추가===============================
         private String addDateFormat(String message) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            String dateTime = sdf.format(now);
            StringBuilder sb = new StringBuilder();
            sb.append(message + "  ( " + dateTime + " )");
            return sb.toString();
         }
         //=======================================================================
      };
      thread.start();
   }

   
   // 필터링 클래스 ============================== 욕설 필터링 ===============================
   public String filtering(String msg) {
      FileInputStream fis;
      InputStreamReader isr;
      BufferedReader bReader;
      try {
         
         fis = new FileInputStream("filtering.txt"); //FileInputStream 객체생성
         isr = new InputStreamReader(fis,"UTF-8"); //InputStream객체 생성
         bReader = new BufferedReader(isr); //Buffered Reader 객체생성
         String words = bReader.readLine(); 
            // 파일에서 한줄씩 읽어와서  words에 저장
         String[] warr = words.split(","); 
            //,를 구분자로 words에 저장되 있는 단어들을 구분해서 각 요소에 저장
         int size= warr.length; //생성된 배열의 길이를 저장
         String filterword = ""; 
         for(int i=0; i<size ; i++) {
            filterword = warr[i]; //한 요소씩 읽어서 filterword에 저장
            filterword = filterword.trim();
            if(msg.contains(filterword)) {
                // 매개변수로 받은 채팅내용에 해당 단어가 포함되어 있는지 확인
               int s = filterword.length(); 
                    //해당 단어(요소에서 읽어 온 단어)의 길이 저장
               String hider = ""; 
               int j=0;
               while(j<s) { 
                    //해당 단어의 길이만큼 *를 데이터로 갖고있는 String 변수 hinder 만들기
                  hider = hider +"*";
                  j++;
               }
               msg = msg.replaceAll(filterword, hider); 
              //대화 내용중 filterword 부분을 hinder로 대체하여 다시 저장
                  
            } //if
         }//for

      }catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return msg; // 욕설에 해당하는 부분을 *로 대체한 대화내용을 전달
   }
   //====================================================================================   
   
   //======================= 날씨 (웹크롤링 이용)===============================================
   
   public String getweather() {
      String result = "";
      try {
         String URL = "https://weather.naver.com/";
         Document doc = Jsoup.connect(URL).get();
      
         Element elem = doc.select(".current").get(0);
         Element elem2 = doc.select(".weather").get(0);
         
         result = elem.text() + "   " + elem2.text();
         
      } catch (Exception e) {
         // TODO: handle exception
         e.printStackTrace();
         }
      
      return result;
}
  
 //======================== 번역 클래스 =======================================================
    public String getTransSentence2(String s){ // 한국어 -> 영어 번역
       String clientId = "wvKuiHzQQGmFd4tEoQD9";//애플리케이션 클라이언트 아이디값";
       String clientSecret = "fenuQmk5s2";//애플리케이션 클라이언트 시크릿값";

       String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
       String text;
       
       try {
          text = URLEncoder.encode(s, "UTF-8");
       } catch (UnsupportedEncodingException e) {
          throw new RuntimeException("인코딩 실패", e);
       }

       Map<String, String> requestHeaders = new HashMap<>();
       requestHeaders.put("X-Naver-Client-Id", clientId);
       requestHeaders.put("X-Naver-Client-Secret", clientSecret);

       String responseBody = post2(apiURL, requestHeaders, text);
       System.out.println("responseBody = " + responseBody);

       return convertToData(responseBody);
    }
    
    public String getTransSentence1(String s){ // 영어 -> 한국어 번역
       String clientId = "wvKuiHzQQGmFd4tEoQD9";//애플리케이션 클라이언트 아이디값";
       String clientSecret = "fenuQmk5s2";//애플리케이션 클라이언트 시크릿값";

       String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
       String text;
       try {
          text = URLEncoder.encode(s, "UTF-8");
       } catch (UnsupportedEncodingException e) {
          throw new RuntimeException("인코딩 실패", e);
       }

       Map<String, String> requestHeaders = new HashMap<>();
       requestHeaders.put("X-Naver-Client-Id", clientId);
       requestHeaders.put("X-Naver-Client-Secret", clientSecret);

       String responseBody = post1(apiURL, requestHeaders, text);
       System.out.println("responseBody = " + responseBody);

       return convertToData(responseBody);
    }
    

    private String convertToData(String str){
       String responseBody = str;

       // JSON 파싱
       JSONObject jsonObj = new JSONObject(responseBody);

       // "result" 객체의 "translatedText" 속성 추출
       String translatedText = jsonObj.getJSONObject("message")
             .getJSONObject("result")
             .getString("translatedText");

       System.out.println("번역값 확인: " + translatedText);

       return translatedText;
    }
    
    private String post1(String apiUrl, Map<String, String> requestHeaders, String text){
       HttpURLConnection con = connect(apiUrl);
       String postParams = "source=en&target=ko&text=" + text; //원본언어: 영어 (en) -> 목적언어: 한국어 (ko)
       try {
          con.setRequestMethod("POST");
          for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
             con.setRequestProperty(header.getKey(), header.getValue());
          }

          con.setDoOutput(true);
          try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
             wr.write(postParams.getBytes());
             wr.flush();
          }

          int responseCode = con.getResponseCode();
          if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
             return readBody(con.getInputStream());
          } else {  // 에러 응답
             return readBody(con.getErrorStream());
          }
       } catch (IOException e) {
          throw new RuntimeException("API 요청과 응답 실패", e);
       } finally {
          con.disconnect();
       }
    }
    
    private String post2(String apiUrl, Map<String, String> requestHeaders, String text){
       HttpURLConnection con = connect(apiUrl);
       String postParams = "source=ko&target=en&text=" + text; //원본언어: 한국어 (ko) -> 목적언어: 영어 (en)
       try {
          con.setRequestMethod("POST");
          for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
             con.setRequestProperty(header.getKey(), header.getValue());
          }

          con.setDoOutput(true);
          try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
             wr.write(postParams.getBytes());
             wr.flush();
          }

          int responseCode = con.getResponseCode();
          if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
             return readBody(con.getInputStream());
          } else {  // 에러 응답
             return readBody(con.getErrorStream());
          }
       } catch (IOException e) {
          throw new RuntimeException("API 요청과 응답 실패", e);
       } finally {
          con.disconnect();
       }
    }

    private HttpURLConnection connect(String apiUrl){
       try {
          URL url = new URL(apiUrl);
          return (HttpURLConnection)url.openConnection();
       } catch (MalformedURLException e) {
          throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
       } catch (IOException e) {
          throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
       }
    }

    private String readBody(InputStream body){
       InputStreamReader streamReader = new InputStreamReader(body);

       try (BufferedReader lineReader = new BufferedReader(streamReader)) {
          StringBuilder responseBody = new StringBuilder();

          String line;
          while ((line = lineReader.readLine()) != null) {
             responseBody.append(line);
          }

          return responseBody.toString();
       } catch (IOException e) {
          throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
       }
    }
    
    //====================================================================================
     
   // 실제로 프로그램을 동작시키는 메서드
   @Override
   public void start(Stage primaryStage) {
      BorderPane root = new BorderPane();
      root.setPadding(new Insets(5));
      
      HBox hbox = new HBox();
      hbox.setSpacing(5);
      
      Scene scene = new Scene(root, 700, 700);
      
      
      Button weatherButton = new Button("오늘의 날씨");
      weatherButton.setStyle("-fx-text-fill: black; -fx-background-color: #FF7F50");
      
      weatherButton.setOnAction(event -> {
         textArea.appendText(getweather() + "\n");
      });
      
      userName = new TextField();
      userName.setPrefWidth(50);
      userName.setPromptText("닉네임을 입력하세요.");
      userName.setStyle("-fx-text-fill: black; -fx-background-color: ");
      HBox.setHgrow(userName, Priority.ALWAYS);
      
      //서버에 ip주소가 들어갈 수 있도록.
      TextField IPText = new TextField("192.168.0.68");
      TextField portText = new TextField("5001");
      portText.setPrefWidth(50);
     
      
      hbox.getChildren().addAll(userName, IPText, portText, weatherButton, connectionButton);
      root.setTop(hbox);
      // 참여인원 목록
      VBox vBox = new VBox();
      vBox.setPadding(new Insets(5));
      vBox.setSpacing(5);
      
      
      namelist = new TextArea();
      namelist.setPrefWidth(100);
      namelist.setPrefHeight(200);
   
      root.setRight(vBox);
      vBox.getChildren().addAll(new Label("떠든사람"),namelist);
      
      textArea = new TextArea();
      textArea.setEditable(false);
      root.setCenter(textArea);
      
      TextField input = new TextField();
      input.setPrefWidth(Double.MAX_VALUE);
      input.setDisable(true);
      
      //======================공지사항====================

        Button uploadButton = new Button("공지사항");             //업로드 버튼
      uploadButton.setOnAction(event ->{
         send("[ 공지사항 ]  " + input.getText());
         input.setText("");
         input.requestFocus();
      });
      
        //===============================================
        
        
      //========================귓속말=======================
      whisperTarget = new TextField();
      whisperTarget.setPromptText("귓속말 상대 이름 입력");
      //===================================================
      
      input.setOnAction(event -> {
         send(userName.getText() + ": " + input.getText());
         input.setText("");
         input.requestFocus();
      });
      
      Button sendButton = new Button("보내기");
//      sendButton.setDisable(true);
      
      sendButton.setOnAction(event ->{
         send(userName.getText() + " : " + input.getText());
         
         input.setText("");
         input.requestFocus();
      });
          
      //=============================새창==============================
      window = primaryStage;
      Button toScene2 = new Button("번역");   
      toScene2.setOnAction(e->window.setScene(scene2));
      toScene2.setStyle("-fx-background-color: #EE82EE");
      
      TextField input2 = new TextField();
      TextField input3 = new TextField();
      
      // layout 2
      Button goBack = new Button("뒤로가기");
      goBack.setStyle(" -fx-background-color: #7FFFD4");
      goBack.setOnAction(e->{
         window.setScene(scene);
         input2.setText("");
         input3.setText("");
         textArea2.setText("");
      });
      
 //     StackPane layout2 = new StackPane();
      BorderPane root2 = new BorderPane();
      root2.setPadding(new Insets(5));
      root2.setStyle(" -fx-background-color: #FFE08C");
      root2.setTop(goBack);
      //root2.getChildren().add(goBack);
      
      input2.setPrefWidth(400);
      
      Button txButton = new Button("한글 -> 영어");
      txButton.setStyle("-fx-background-color: lightblue");
      BorderPane root3 = new BorderPane();
      root3.setLeft(input2);
      root3.setRight(txButton);
      
      //
      
      input3.setPrefWidth(400);
      Button txButton2 = new Button("영어 -> 한글");
      txButton2.setStyle("-fx-background-color: lightblue");
    
      BorderPane root4 = new BorderPane();
      root4.setLeft(input3);
      root4.setRight(txButton2);
      
      root3.setBottom(root4);
      
      txButton.setOnAction(event -> {
         String result = getTransSentence2(input2.getText());
         textArea2.appendText(result + "\n");
      });
      
      
      txButton2.setOnAction(event -> {
         String result = getTransSentence1(input3.getText());
         textArea2.appendText(result + "\n");
      });
      
      root2.setCenter(root3);
      
      
      Button cbButton = new Button("복사하기");
      root2.setBottom(cbButton);
      cbButton.setOnAction(event -> {
         String text = textArea2.getText();
         StringSelection selection = new StringSelection(text);
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         clipboard.setContents(selection, null);
         textArea2.setText("");
      });
      cbButton.setStyle("-fx-background-color: #90EE90");
      
      root4.setBottom(cbButton);
      
      
      textArea2 = new TextArea();
      
      root2.setBottom(textArea2);
      

      scene2 = new Scene(root2, 500,300);
      //===============================================================
   
      connectionButton.setOnAction(event -> {
         if(connectionButton.getText().equals("접속하기")) {
            int port = 9876;
            try {
               port = Integer.parseInt(portText.getText());
            }catch(Exception e) {
               e.printStackTrace();
            }
            startClient(IPText.getText(), port);
            Platform.runLater(() -> {
               textArea.appendText("[채팅방접속]\n");
               textArea.appendText(userName.getText() + "님 반갑습니다! \n");
            });
            connectionButton.setText("종료하기");
            input.setDisable(false);
            input.requestFocus();
         }else {
            stopClient();
            Platform.runLater(() ->{
               textArea.appendText("[채팅방 퇴장]\n");
               textArea.appendText(userName.getText() + "님이 나가셨습니다.! \n");
            });
            connectionButton.setText("접속하기");
            input.setDisable(true);
            sendButton.setDisable(true);
         }
      });
   
      BorderPane pane = new BorderPane();
      whisperTarget.setStyle("-fx-background-color: #CEF279");
      pane.setTop(whisperTarget);      //<--------------추가
      pane.setLeft(toScene2);         //<-------------번역 버튼
      connectionButton.setStyle("-fx-text-fill: black; -fx-background-color: #79ABFF");
      
      pane.setBottom(input);
      pane.setRight(sendButton);
      pane.setCenter(uploadButton);   //————— 공지사항 업로드 버튼추가내용
      uploadButton.setStyle(" -fx-background-color: #FF8383");
      sendButton.setStyle("-fx-text-fill: black; -fx-background-color: lightblue");
//      pane.setTop(count);

      root.setBottom(pane);
      
      root.setStyle("-fx-text-fill: red; -fx-background-color: #FFE08C");
      
      primaryStage.setTitle(" [클라이언트] ");
      primaryStage.setScene(scene);
      primaryStage.setOnCloseRequest(event -> stopClient());
      primaryStage.show();
      
      connectionButton.requestFocus();
      
   }

   // 프로그램의 진입점.
   public static void main(String[] args) {
      launch(args);
   }
}