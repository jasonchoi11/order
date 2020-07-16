package rental;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="OrderState_table")
public class OrderState {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long orderId;
        private String status;
        private String contractDate;
        private Long deliveryId;
        private String deliveryStatus;
        private Long checkId;
        private String checkDate;
        private String checkStatus;
        private Long productId;
        private Integer score;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public String getContractDate() {
            return contractDate;
        }

        public void setContractDate(String contractDate) {
            this.contractDate = contractDate;
        }
        public Long getDeliveryId() {
            return deliveryId;
        }

        public void setDeliveryId(Long deliveryId) {
            this.deliveryId = deliveryId;
        }
        public String getDeliveryStatus() {
            return deliveryStatus;
        }

        public void setDeliveryStatus(String deliveryStatus) {
            this.deliveryStatus = deliveryStatus;
        }
        public Long getCheckId() {
            return checkId;
        }

        public void setCheckId(Long checkId) {
            this.checkId = checkId;
        }
        public String getCheckDate() {
            return checkDate;
        }

        public void setCheckDate(String checkDate) {
            this.checkDate = checkDate;
        }
        public String getCheckStatus() {
            return checkStatus;
        }

        public void setCheckStatus(String checkStatus) {
            this.checkStatus = checkStatus;
        }
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }
        public Integer getScore() {
        return score;
    }

         public void setScore(Integer score) {
        this.score = score;
    }

}
