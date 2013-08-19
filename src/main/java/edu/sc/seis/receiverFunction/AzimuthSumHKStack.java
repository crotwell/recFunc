package edu.sc.seis.receiverFunction;

import java.util.List;
import java.util.Set;

import edu.iris.Fissures.model.QuantityImpl;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;

public class AzimuthSumHKStack extends SumHKStack {

    /** hibernate */
    protected AzimuthSumHKStack() {}
    
    public AzimuthSumHKStack(float minPercentMatch,
                             QuantityImpl smallestH,
                             HKStack sum,
                             float hVariance,
                             float kVariance,
                             int numEQ,
                             List<ReceiverFunctionResult> individuals,
                             Set<RejectedMaxima> rejects,
                             float azimuthCenter,
                             float azimuthWidth) {
        super(minPercentMatch,
              smallestH,
              sum,
              hVariance,
              kVariance,
              numEQ,
              individuals,
              rejects);
        this.azimuthCenter = azimuthCenter;
        this.azimuthWidth = azimuthWidth;
    }

    public static AzimuthSumHKStack calculateForPhase(List<ReceiverFunctionResult> stackList,
                                                      QuantityImpl smallestH,
                                                      float minPercentMatch,
                                                      boolean usePhaseWeight,
                                                      Set<RejectedMaxima> rejects,
                                                      boolean doBootstrap,
                                                      int bootstrapIterations,
                                                      String phase,
                                                      float azimuth,
                                                      float azWidth) {
        SumHKStack sumStack = SumHKStack.calculateForPhase(stackList,
                                                           smallestH,
                                                           minPercentMatch,
                                                           usePhaseWeight,
                                                           rejects,
                                                           doBootstrap,
                                                           bootstrapIterations,
                                                           phase);
        return new AzimuthSumHKStack(sumStack.getMinPercentMatch(),
                                     sumStack.getSmallestH(),
                                     sumStack.getSum(),
                                     (float)sumStack.getHVariance(),
                                     (float)sumStack.getKVariance(),
                                     sumStack.getNumEQ(),
                                     sumStack.getIndividuals(),
                                     sumStack.getRejectedMaxima(),
                                     azimuth,
                                     azWidth);
    }

    float azimuthCenter;

    float azimuthWidth;

    public float getAzimuthCenter() {
        return azimuthCenter;
    }

    public float getAzimuthWidth() {
        return azimuthWidth;
    }

    
    protected void setAzimuthCenter(float azimuth) {
        this.azimuthCenter = azimuth;
    }

    
    protected void setAzimuthWidth(float azWidth) {
        this.azimuthWidth = azWidth;
    }
    
}
