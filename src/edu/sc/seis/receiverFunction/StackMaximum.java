package edu.sc.seis.receiverFunction;

import java.text.DecimalFormat;

import edu.iris.Fissures.model.QuantityImpl;
import edu.sc.seis.sod.status.FissuresFormatter;


public class StackMaximum {
    
    int hIndex;
    
    int kIndex;
    
    float complexityResidual;
    
    float complexityOriginal;
    
    float maxValue;

    QuantityImpl hValue;
    
    float kValue;
    
    public StackMaximum(int hIndex, QuantityImpl hValue, int kIndex, float kValue, float maxValue, float complexityOriginal, float complexityResidual) {
        this.hIndex = hIndex;
        this.hValue = hValue;
        this.kIndex = kIndex;
        this.kValue = kValue;
        this.maxValue = maxValue;
        this.complexityResidual = complexityResidual;
        this.complexityOriginal = complexityOriginal;
    }
    
    public QuantityImpl getHValue() {
        return hValue;
    }
    
    public float getKValue() {
        return kValue;
    }


    public float getComplexityOriginal() {
        return complexityOriginal;
    }

    
    public float getComplexityResidual() {
        return complexityResidual;
    }

    
    public int getHIndex() {
        return hIndex;
    }

    
    public int getKIndex() {
        return kIndex;
    }
    
    public float getMaxValue() {
        return maxValue;
    }

    public String formatHValue() {
        return FissuresFormatter.formatQuantity(getHValue());
    }

    public String formatMaxValueK() {
        return vpvsFormat.format(getKValue());
    }
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");
}
