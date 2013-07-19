package eu.ttbox.geoping.encoder.params.encoder;

import eu.ttbox.geoping.encoder.adapter.EncoderAdapter;
import eu.ttbox.geoping.encoder.model.MessageParamType;
import eu.ttbox.geoping.encoder.params.IParamEncoder;
import eu.ttbox.geoping.encoder.params.helper.IntegerEncoded;

public class IntegerParamEncoder implements IParamEncoder {

    public final int radix;


    // ===========================================================
    //   Contructor
    // ===========================================================



    public IntegerParamEncoder( ) {
        this( IntegerEncoded.MAX_RADIX);
    }


    public IntegerParamEncoder(int radix) {
        this.radix = radix;
    }

    // ===========================================================
    //   Encoder - Decoder Accessor
    // ===========================================================

    @Override
    public boolean writeTo(EncoderAdapter src,  StringBuilder dest, MessageParamType field, char smsFieldName  ) {
        return writeTo(src, dest, field, smsFieldName, true);
    }

    @Override
    public boolean writeTo(EncoderAdapter src,  StringBuilder dest, MessageParamType field, char smsFieldName, boolean isSmsFieldName ) {
        boolean isWrite = false;
        Integer value =  (Integer) src.get(field.dbFieldName) ;
        if (value != null) {
            String valueString = IntegerEncoded.toString(value, radix);
            if (isSmsFieldName) {
                dest.append( smsFieldName);
            }
            dest.append(value);
            isWrite = true;
        }
        return isWrite;
    }
}
