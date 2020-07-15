![image](https://user-images.githubusercontent.com/65527020/87420811-e27f6e80-c610-11ea-961b-4895e2532b9b.png)

# 정수기 렌탈 서비스

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 시스템입니다.

# 구현 Repository 

총 7개
1. https://github.com/SK-teamC/order
2. https://github.com/SK-teamC/delivery
3. https://github.com/SK-teamC/product
4. https://github.com/SK-teamC/payment
5. https://github.com/SK-teamC/check
6. https://github.com/SK-teamC/notify
7. https://github.com/SK-teamC/gateway

# Table of contents

- [예제 - 정수기렌탈서비스](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

정수기 렌탈 서비스

기능적 요구사항
1. 고객이 정수기를 선택하여 주문한다
1. 고객이 결제한다
1. 주문이 완료 되면 주문 내역이 배송팀에게 전달된다
1. 배송팀이 확인하여 정수기를 배달한다
1. 주문이 확정되면 점검일정이 1개월 뒤로 자동 설정 된다.
1. 주문이 확정되면 상품수량이 감소한다.
1. 주문이 취소되면 배달이 취소된다
1. 주문이 취소되면 결제가 취소되며 해당 상품의 재고 수량을 증가시킨다.
1. 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다  Sync 호출 
1. 장애격리
    1. 상품관리 기능이 수행되지 않더라도 주문은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 고객이 자주 주문상태를 확인할 수 있는 주문시스템(프론트엔드)에서 확인할 수 있어야 한다  CQRS
    1. 주문상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven


# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  ![image](https://user-images.githubusercontent.com/65527020/87506005-42bff000-c6a5-11ea-852a-268903cae140.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://msaez.io/#/storming/yMtAcGqlvFOF8bDmBRcWQNX1Li43/every/081061689478e7b4511cf7fb98c59f3d/-MCBUp0HGJ8THO3TFeHH


### 이벤트 도출
![image](https://user-images.githubusercontent.com/65527020/87421918-b06f0c00-c612-11ea-8c9d-59099342007c.png)

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/65527020/87423757-ca5e1e00-c615-11ea-87e2-a9476ab33ca2.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/65527020/87424571-22e1eb00-c617-11ea-9e95-fd9536ea2373.png)

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/65527020/87426184-caf8b380-c619-11ea-9855-5ff24de69f82.png)

    - 도메인 서열 분리 
        - Core Domain:  order, product : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 order 의 경우 1주일 1회 미만, product 의 경우 1개월 1회 미만
        - Supporting Domain:   delivery, check : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:  payment : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)
                           
### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![image](https://user-images.githubusercontent.com/65527020/87428150-df8a7b00-c61c-11ea-99f5-16b32314aed9.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/65527020/87429291-86234b80-c61e-11ea-97d4-7ebd227bcdae.png)

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/65527020/87429868-60e30d00-c61f-11ea-9b4e-a3bdf096b686.png)

    - View Model 추가

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/65527020/87490914-d253a780-c681-11ea-9ffb-cd4d16a55e6f.png)

    - 고객이 정수기를 선택하여 주문한다 (ok)
    - 고객이 결제한다 (ok)
    - 주문이 완료 되면 주문 내역이 배송팀에게 전달된다 (ok)
    - 배송팀이 확인하여 정수기를 배달한다 (ok)
    - 주문이 확정되면 점검일정이 1개월 뒤로 자동 설정 된다 (ok)
    - 주문이 확정되면 상품수량이 감소한다 (ok)

![image](https://user-images.githubusercontent.com/65527020/87491628-9c172780-c683-11ea-99e2-afdcf6c908c1.png)

    - 주문이 취소되면 배달이 취소된다 (ok)
    - 주문이 취소되면 결제가 취소되며 해당 상품의 재고 수량을 증가시킨다 (ok)
    - 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다 (?)

### 모델 수정

![image](https://user-images.githubusercontent.com/65527020/87492271-2744ed00-c685-11ea-9232-8692c691f815.png)
    
    - 수정된 모델은 모든 요구사항을 커버함.

### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/65527020/87492623-ebf6ee00-c685-11ea-982c-542b9133f393.png)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 고객 주문시 결제처리:  결제가 완료되지 않은 주문은 절대 받지 않는다. ACID 트랜잭션 적용. 주문완료시 결제처리에 대해서는 Request-Response 방식 처리
        - 상품재고 변경 및 배송처리:  order 에서 delivery, product 마이크로서비스로 주문요청이 전달되는 과정에 있어서 delivery, product 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
        - 나머지 모든 inter-microservice 트랜잭션: 주문상태, 배송상태 등 모든 이벤트에 대해 카톡을 처리하는 등, 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.




## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/65527020/87419081-d3e38800-c60d-11ea-94d9-7a04b3e2a87e.png)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd order
mvn spring-boot:run

cd payment
mvn spring-boot:run 

cd product
mvn spring-boot:run  

cd check
mvn spring-boot:run

cd delivery
mvn spring-boot:run

cd notify
mvn spring-boot:run

cd gateway
mvn spring-boot:run

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 pay 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 하지만, 일부 구현에 있어서 영문이 아닌 경우는 실행이 불가능한 경우가 있기 때문에 계속 사용할 방법은 아닌것 같다. (Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생하는 것을 확인하였다)

```
package rental;

import org.springframework.beans.BeanUtils;
import rental.external.Payment;
import rental.external.PaymentService;

import javax.persistence.*;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long productId;
    private Integer qty;
    private String contractDate;
    private String period;
    private Integer rentalPrice;
    private String status;

    @PostPersist
    public void onPostPersist(){

        //결재 요청 후 kafka
        Payment payment = new Payment();
        payment.setOrderId(getId());
        payment.setRentalPrice(getRentalPrice());
        payment.setStatus("승인요청");
        payment.setApprovalDate(getContractDate());
        OrderApplication.applicationContext.getBean(PaymentService.class)
            .approval(payment);

        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

    }

    @PostUpdate
    public void onPostUpdate(){
        OrderCanceled orderCanceled = new OrderCanceled();
        BeanUtils.copyProperties(this, orderCanceled);
        orderCanceled.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getContractDate() {
        return contractDate;
    }

    public void setContractDate(String contractDate) {
        this.contractDate = contractDate;
    }
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
    public Integer getRentalPrice() {
        return rentalPrice;
    }

    public void setRentalPrice(Integer rentalPrice) {
        this.rentalPrice = rentalPrice;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package rental;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{
}
```
- 적용 후 REST API 의 테스트
```
# order 서비스의 주문처리
http POST localhost:8081/orders productId=1 rentalPrice=10000 contractDate=20200715 state=ordered

# delivery 서비스의 배송처리
http POST localhost:8082/deliverys id=1

# 주문 상태 확인
http localhost:8081/orderStates/1

```


## 폴리글랏 퍼시스턴스


## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 주문(order)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (Payment) PaymentService.java

package rental.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="Payment", url="http://Payment:8080")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void approval(@RequestBody Payment payment);

}
```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){

        //결재 요청 후 kafka
        Payment payment = new Payment();
        payment.setOrderId(getId());
        payment.setRentalPrice(getRentalPrice());
        payment.setStatus("승인요청");
        payment.setApprovalDate(getContractDate());
        OrderApplication.applicationContext.getBean(PaymentService.class)
            .approval(payment);

        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

    }
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:

```
# 결제 (pay) 서비스를 잠시 내려놓음 (ctrl+c)

#주문처리
http POST localhost:8081/orders productId=1001 rentalPrice=10000 contractDate=20200715   #Fail
http POST localhost:8081/orders productId=1002 rentalPrice=20000 contractDate=20200715   #Fail

#결제서비스 재기동
cd payment
mvn spring-boot:run

#주문처리
http POST localhost:8081/orders productId=1001 rentalPrice=10000 contractDate=20200714   #Success
http POST localhost:8081/orders productId=1002 rentalPrice=20000 contractDate=20200714   #Success
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

결제가 이루어진 후에 상품시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 상품 시스템의 처리를 위하여 결제주문이 블로킹 되지 않도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package rental;

import org.springframework.beans.BeanUtils;
import rental.external.Payment;
import rental.external.PaymentService;

import javax.persistence.*;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long productId;
    private Integer qty;
    private String contractDate;
    private String period;
    private Integer rentalPrice;
    private String status;

    @PostPersist
    public void onPostPersist(){

        //결재 요청 후 kafka
        Payment payment = new Payment();
        payment.setOrderId(getId());
        payment.setRentalPrice(getRentalPrice());
        payment.setStatus("승인요청");
        payment.setApprovalDate(getContractDate());
        OrderApplication.applicationContext.getBean(PaymentService.class)
            .approval(payment);

        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

    }
```
- 상품 서비스에서는 주문처리 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package rental;

```
@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    @Autowired
    ProductRepository productRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCanceled_ProductChange(@Payload OrderCanceled orderCanceled){

        if(orderCanceled.isMe()){
            Product product = null;
            Optional<Product> optional = productRepository.findById(orderCanceled.getProductId());
            if(optional.isPresent()) {
                product = optional.get();
                product.setId(orderCanceled.getProductId());
                product.setAmount(product.getAmount() != null ? product.getAmount().intValue() + 1 : 0);
                productRepository.save(product);
            }
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_ProductChange(@Payload Ordered ordered){
        if(ordered.isMe()){
            Product product = null;
            Optional<Product> optional = productRepository.findById(ordered.getProductId());
            if(optional.isPresent()) {
                product = optional.get();

                product.setId(ordered.getProductId());
                product.setAmount(product.getAmount() != null ? product.getAmount().intValue() - 1 : 0);
                productRepository.save(product);
            }
        }
    }
}

```
실제 구현을 하자면, 카톡 등으로 주문,배송,점검일정에 대한 노티를 받는다.
  
```
package rental;

```
@Service
public class PolicyHandler{
    @Autowired NotifyRepository notifyRepository;
    private static final String TOPIC_NAME = "rental";
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    Message message = new Message();

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_Notify(@Payload Ordered ordered){
        if(ordered.isMe()){
            message.setMessage( "주문이 완료되었습니다. 주문번호 : " + ordered.getId() + ", 상품번호 : " + ordered.getProductId() );
            notifyRepository.save(message);
        }
    }

```

상품 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 상품시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 배송 서비스 (delivery) 를 잠시 내려놓음 (ctrl+c)

#주문처리
http localhost:8081/orders productId=1 rentalPrice=10000 contractDate=20200715   #Success
http localhost:8081/orders productId=2 rentalPrice=20000 contractDate=20200715   #Success

#주문상태 확인
http localhost:8081/orders     # 주문상태 안바뀜 확인

#배송 서비스 기동
cd delivery
mvn spring-boot:run

#주문상태 확인
http localhost:8081/orderStates     # 모든 주문의 상태가 "배송됨"으로 확인
```

# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.


## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 단말앱(app)-->결제(pay) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스(결제:pay) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# (payment) Payment.java (Entity)

    @PrePersist
    public void onPrePersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ...
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시

```
$ siege -c20 -t10S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"productId": "1001"}'

** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.73 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.75 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.77 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.97 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.81 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.87 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.12 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.16 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.17 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.26 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     1.25 secs:     207 bytes ==> POST http://localhost:8081/orders

* 요청이 과도하여 CB를 동작함 요청을 차단

HTTP/1.1 201     1.10 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.10 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.10 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.60 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     1.14 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.66 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.87 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.94 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.03 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.69 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.03 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.05 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.06 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.09 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.08 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.44 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.06 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.00 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.11 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.12 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.06 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.04 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.15 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.18 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.86 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.37 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.31 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.68 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.34 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders

* 요청을 어느정도 돌려보내고나니, 기존에 밀린 일들이 처리되었고, 회로를 닫아 요청을 다시 받기 시작

HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.04 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.88 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.14 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.86 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.94 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.97 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.96 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.03 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.96 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.96 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.11 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.96 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.11 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.08 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.06 secs:     280 bytes ==> POST http://order:8080/orders

* 다시 요청이 쌓이기 시작하여 건당 처리시간이 610 밀리를 살짝 넘기기 시작 => 회로 열기 => 요청 실패처리

HTTP/1.1 500     1.93 secs:     248 bytes ==> POST http://localhost:8081/orders    
HTTP/1.1 500     1.92 secs:     248 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 500     1.93 secs:     248 bytes ==> POST http://localhost:8081/orders

* 생각보다 빨리 상태 호전됨 - (건당 (쓰레드당) 처리시간이 610 밀리 미만으로 회복) => 요청 수락

HTTP/1.1 201     2.24 secs:     207 bytes ==> POST http://localhost:8081/orders  
HTTP/1.1 201     2.32 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.16 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.19 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.21 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.29 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.30 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.38 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.59 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.61 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.62 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     2.64 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.01 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.27 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.33 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.45 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.52 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.57 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.69 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     4.69 secs:     207 bytes ==> POST http://localhost:8081/orders

* 이후 이러한 패턴이 계속 반복되면서 시스템은 도미노 현상이나 자원 소모의 폭주 없이 잘 운영됨


HTTP/1.1 201     0.91 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.04 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.24 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.55 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.11 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.80 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.81 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.10 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.96 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 500     0.51 secs:     248 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.83 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.84 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.94 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.98 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.15 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.06 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.91 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.30 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.02 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.94 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.95 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.05 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.13 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.98 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     0.92 secs:     280 bytes ==> POST http://order:8080/orders
HTTP/1.1 201     1.04 secs:     280 bytes ==> POST http://order:8080/orders

```
Lifting the server siege...
Transactions:                    156 hits
Availability:                  77.61 %
Elapsed time:                   9.59 secs
Data transferred:               0.05 MB
Response time:                  1.15 secs
Transaction rate:              16.27 trans/sec
Throughput:                     0.01 MB/sec
Concurrency:                   18.74
Successful transactions:         156
Failed transactions:              45
Longest transaction:            1.72
Shortest transaction:           0.03
```
- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 63.55% 가 성공하였고, 23%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Retry 설정과 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.

- Retry 의 설정 (istio)
- Availability 가 높아진 것을 확인 (siege)

### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 결제서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy payment --min=1 --max=3 --cpu-percent=15
```
- CB 에서 했던 방식대로 워크로드를 2분 동안 걸어준다.
```
siege -c100 -t120S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"productId": "1001"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy order -w
```
- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:
```
NAME    DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
payment     1         1         1            1           17s
payment     1         2         1            1           45s
payment     1         4         1            1           1m
:
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
```
Transactions:		        5078 hits
Availability:		       92.45 %
Elapsed time:		       120 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02
```


## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c100 -t120S -r10 --content-type "application/json" 'http://localhost:8081/orders POST {"productId": "1001"}'

** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.68 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
HTTP/1.1 201     0.70 secs:     207 bytes ==> POST http://localhost:8081/orders
:

```

- 새버전으로의 배포 시작
```
kubectl set image ...
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```
Transactions:		        3078 hits
Availability:		       70.45 %
Elapsed time:		       120 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02

```
배포기간중 Availability 가 평소 100%에서 70% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:

kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:
```
Transactions:		        3078 hits
Availability:		       100 %
Elapsed time:		       120 secs
Data transferred:	        0.34 MB
Response time:		        5.60 secs
Transaction rate:	       17.15 trans/sec
Throughput:		        0.01 MB/sec
Concurrency:		       96.02

```

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.
