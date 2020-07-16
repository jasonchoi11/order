package rental;

public class SatisfactionSurveyed extends AbstractEvent {

    private Long id;
    private Long checkId;
    private Integer score;

    public SatisfactionSurveyed(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }
    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}

