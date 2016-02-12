package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class DanaRMessages {
    private static Logger log = LoggerFactory.getLogger(DanaRMessages.class);

    public static HashMap<Integer,DanaRMessage> sDanaRMessages;
    
    static {
        sDanaRMessages = new HashMap<Integer,DanaRMessage>();
        sDanaRMessages.put(Integer.valueOf(0x3001), new MsgPCCommStart("CMD_CONNECT"));
        sDanaRMessages.put(Integer.valueOf(0x3002), new MsgPCCommStop("CMD_DISCONNECT"));

        sDanaRMessages.put(Integer.valueOf(0x3101), new DanaRMessage("CMD_HISTORY_MEAL_INS"));
        sDanaRMessages.put(Integer.valueOf(0x3102), new DanaRMessage("CMD_HISTORY_DAY_INS"));
        sDanaRMessages.put(Integer.valueOf(0x3103), new DanaRMessage("CMD_HISTORY_AIR_SUB"));
        sDanaRMessages.put(Integer.valueOf(0x3104), new DanaRMessage("CMD_HISTORY_GLUCOSE"));
        sDanaRMessages.put(Integer.valueOf(0x3105), new DanaRMessage("CMD_HISTORY_ALARM"));
        sDanaRMessages.put(Integer.valueOf(0x3106), new DanaRMessage("CMD_HISTORY_ERROR"));
        sDanaRMessages.put(Integer.valueOf(0x3107), new DanaRMessage("CMD_HISTORY_CARBOHY"));
        sDanaRMessages.put(Integer.valueOf(0x3108), new DanaRMessage("CMD_HISTORY_REFILL"));
        sDanaRMessages.put(Integer.valueOf(0x3109), new DanaRMessage("CMD_HISTORY_SUSPEND"));
        sDanaRMessages.put(Integer.valueOf(0x310a), new DanaRMessage("CMD_HISTORY_BASAL_HOUR"));
        sDanaRMessages.put(Integer.valueOf(0x310b), new DanaRMessage("CMD_HISTORY_TB"));
        sDanaRMessages.put(Integer.valueOf(0x31f1), new DanaRMessage("CMD_HISTORY_DONT_USED"));
        sDanaRMessages.put(Integer.valueOf(0x31f2), new DanaRMessage("CMD_HISTORY_LAST_T_R"));
        sDanaRMessages.put(Integer.valueOf(0x31f3), new DanaRMessage("CMD_HISTORY_LAST_T_S"));

        sDanaRMessages.put(Integer.valueOf(0x0501), new DanaRMessage("CMD_HISPAGE_MEAL_INS"));
        sDanaRMessages.put(Integer.valueOf(0x0502), new DanaRMessage("CMD_HISPAGE_DAY_INS"));
        sDanaRMessages.put(Integer.valueOf(0x0503), new DanaRMessage("CMD_HISPAGE_AIR_SUB"));
        sDanaRMessages.put(Integer.valueOf(0x0504), new DanaRMessage("CMD_HISPAGE_GLUCOSE"));
        sDanaRMessages.put(Integer.valueOf(0x0505), new DanaRMessage("CMD_HISPAGE_ALARM"));
        sDanaRMessages.put(Integer.valueOf(0x0506), new DanaRMessage("CMD_HISPAGE_ERROR"));
        sDanaRMessages.put(Integer.valueOf(0x0507), new DanaRMessage("CMD_HISPAGE_CARBOHY"));
        sDanaRMessages.put(Integer.valueOf(0x0508), new DanaRMessage("CMD_HISPAGE_REFILL"));
        sDanaRMessages.put(Integer.valueOf(0x050a), new DanaRMessage("CMD_HISPAGE_DAILTY_PRE_DATA"));
        sDanaRMessages.put(Integer.valueOf(0x050b), new DanaRMessage("CMD_HISPAGE_BOLUS_AVG"));
        sDanaRMessages.put(Integer.valueOf(0x050c), new DanaRMessage("CMD_HISPAGE_BASAL_RECORD"));
        sDanaRMessages.put(Integer.valueOf(0x050d), new DanaRMessage("CMD_HISPAGE_TB"));

        sDanaRMessages.put(Integer.valueOf(0x3201), new DanaRMessage("CMD_SETTING_V_MEAL_INS_I"));
        sDanaRMessages.put(Integer.valueOf(0x3202), new DanaRMessage("CMD_SETTING_V_Based_INS_I"));
        sDanaRMessages.put(Integer.valueOf(0x3203), new DanaRMessage("CMD_SETTING_V_MEAL_SETTING_I"));
        sDanaRMessages.put(Integer.valueOf(0x3204), new DanaRMessage("CMD_SETTING_V_CCC_I"));
        sDanaRMessages.put(Integer.valueOf(0x3205), new DanaRMessage("CMD_SETTING_V_MAX_VALUE_I"));
        sDanaRMessages.put(Integer.valueOf(0x3206), new DanaRMessage("CMD_SETTING_V_BASAL_PROFILE_ALL"));
        sDanaRMessages.put(Integer.valueOf(0x3207), new DanaRMessage("CMD_SETTING_V_SHIPPING_I"));
        sDanaRMessages.put(Integer.valueOf(0x3208), new DanaRMessage("CMD_SETTING_V_CLOGGIN_SENS_I"));
        sDanaRMessages.put(Integer.valueOf(0x3209), new DanaRMessage("CMD_SETTING_V_GLUCOSEandEASY"));
        sDanaRMessages.put(Integer.valueOf(0x320a), new MsgStatusTime("CMD_SETTING_V_TIME_I"));
        sDanaRMessages.put(Integer.valueOf(0x320b), new DanaRMessage("CMD_SETTING_V_USER_OPTIONS"));
        sDanaRMessages.put(Integer.valueOf(0x320c), new DanaRMessage("CMD_SETTING_V_PROFILE_NUMBER"));
        sDanaRMessages.put(Integer.valueOf(0x320d), new DanaRMessage("CMD_SETTING_V_CIR_CF_VALUE"));

        sDanaRMessages.put(Integer.valueOf(0x3301), new DanaRMessage("CMD_SETTING_MEAL_INS_S"));
        sDanaRMessages.put(Integer.valueOf(0x3302), new DanaRMessage("CMD_SETTING_Based_INS_S"));
        sDanaRMessages.put(Integer.valueOf(0x3303), new DanaRMessage("CMD_SETTING_MEAL_SETTING_S"));
        sDanaRMessages.put(Integer.valueOf(0x3304), new DanaRMessage("CMD_SETTING_CCC_S"));
        sDanaRMessages.put(Integer.valueOf(0x3305), new DanaRMessage("CMD_SETTING_MAX_VALUE_S"));
        sDanaRMessages.put(Integer.valueOf(0x3306), new MsgSetBasalProfile("CMD_SETTING_BASAL_PROFILE_S"));
        sDanaRMessages.put(Integer.valueOf(0x3307), new DanaRMessage("CMD_SETTING_SHIPPING_S"));
        sDanaRMessages.put(Integer.valueOf(0x3308), new DanaRMessage("CMD_SETTING_CLOGGIN_SENS_S"));
        sDanaRMessages.put(Integer.valueOf(0x3309), new DanaRMessage("CMD_SETTING_GLUCOSEandEASY_S"));
        sDanaRMessages.put(Integer.valueOf(0x330a), new DanaRMessage("CMD_SETTING_TIME_S"));
        sDanaRMessages.put(Integer.valueOf(0x330b), new DanaRMessage("CMD_SETTING_USER_OPTIONS_S"));
        sDanaRMessages.put(Integer.valueOf(0x330c), new MsgSetActivateBasalProfile("CMD_SETTING_PROFILE_NUMBER_S"));
        sDanaRMessages.put(Integer.valueOf(0x330d), new DanaRMessage("CMD_SETTING_CIR_CF_VALUE_S"));

        sDanaRMessages.put(Integer.valueOf(0x0101), new DanaRMessage("CMD_MEALINS_STOP"));
        sDanaRMessages.put(Integer.valueOf(0x0102), new DanaRMessage("CMD_MEALINS_START_DATA"));
        sDanaRMessages.put(Integer.valueOf(0x0103), new DanaRMessage("CMD_MEALINS_START_NODATA"));
        sDanaRMessages.put(Integer.valueOf(0x0104), new DanaRMessage("CMD_MEALINS_START_DATA_SPEED"));
        sDanaRMessages.put(Integer.valueOf(0x0105), new DanaRMessage("CMD_MEALINS_START_NODATA_SPEED"));

        sDanaRMessages.put(Integer.valueOf(0x0201), new DanaRMessage("CMD_PUMP_ACT_INS_VALUE"));
        sDanaRMessages.put(Integer.valueOf(0x0202), new DanaRMessage("CMD_PUMP_THIS_REMAINDER_MEAL_INS"));
        sDanaRMessages.put(Integer.valueOf(0x0203), new DanaRMessage("CMD_PUMP_BASE_SET"));
        sDanaRMessages.put(Integer.valueOf(0x0204), new DanaRMessage("CMD_PUMP_CALCULATION_SETTING"));
        sDanaRMessages.put(Integer.valueOf(0x0205), new DanaRMessage("CMD_PUMP_EXERCISE_MODE"));
        sDanaRMessages.put(Integer.valueOf(0x0206), new DanaRMessage("CMD_PUMP_MEAL_INS_I"));

        sDanaRMessages.put(Integer.valueOf(0x0207), new DanaRMessage("CMD_PUMP_EXPANS_INS_I"));
        sDanaRMessages.put(Integer.valueOf(0x0208), new DanaRMessage("CMD_PUMP_EXPANS_INS_RQ"));

        sDanaRMessages.put(Integer.valueOf(0x0209), new DanaRMessage("CMD_PUMP_DUAL_INS_RQ"));
        sDanaRMessages.put(Integer.valueOf(0x020a), new MsgStatusBasic("CMD_PUMP_INITVIEW_I"));
        sDanaRMessages.put(Integer.valueOf(0x020b), new MsgStatus("CMD_PUMP_STATUS"));
        sDanaRMessages.put(Integer.valueOf(0x020c), new DanaRMessage("CMD_PUMP_CAR_N_CIR"));

        sDanaRMessages.put(Integer.valueOf(0x0301), new MsgStatusTime_InitialConnection("CMD_PUMPINIT_TIME_INFO"));
        sDanaRMessages.put(Integer.valueOf(0x0302), new DanaRMessage("CMD_PUMPINIT_BOLUS_INFO"));
        sDanaRMessages.put(Integer.valueOf(0x0303), new MsgStatusBasic_InitialConnection("CMD_PUMPINIT_INIT_INFO"));
        sDanaRMessages.put(Integer.valueOf(0x0304), new DanaRMessage("CMD_PUMPINIT_OPTION"));

        sDanaRMessages.put(Integer.valueOf(0x0401), new MsgTempBasalStart("CMD_PUMPSET_EXERCISE_S"));
        sDanaRMessages.put(Integer.valueOf(0x0402), new DanaRMessage("CMD_PUMPSET_HIS_S"));
        sDanaRMessages.put(Integer.valueOf(0x0403), new MsgTempBasalStop("CMD_PUMPSET_EXERCISE_STOP"));

        sDanaRMessages.put(Integer.valueOf(0x0404), new DanaRMessage("CMD_PUMPSET_PAUSE"));
        sDanaRMessages.put(Integer.valueOf(0x0405), new DanaRMessage("CMD_PUMPSET_PAUSE_STOP"));

        sDanaRMessages.put(Integer.valueOf(0x0406), new MsgExtendedBolusStop("CMD_PUMPSET_EXPANS_INS_STOP"));
        sDanaRMessages.put(Integer.valueOf(0x0407), new MsgExtendedBolusStart("CMD_PUMPSET_EXPANS_INS_S"));

        sDanaRMessages.put(Integer.valueOf(0x0408), new DanaRMessage("CMD_PUMPSET_DUAL_S"));
        sDanaRMessages.put(Integer.valueOf(0x0409), new DanaRMessage("CMD_PUMPSET_EASY_OFF"));


        sDanaRMessages.put(Integer.valueOf(0x0601), new DanaRMessage("CMD_PUMPOWAY_SYSTEM_STATUS"));
        sDanaRMessages.put(Integer.valueOf(0x0602), new DanaRMessage("CMD_PUMPOWAY_GLUCOSE_ALARM"));
        sDanaRMessages.put(Integer.valueOf(0x0603), new DanaRMessage("CMD_PUMPOWAY_LOW_INSULIN_ALARM"));
        sDanaRMessages.put(Integer.valueOf(0x0610), new DanaRMessage("CMD_PUMP_ALARM_TIEOUT"));

        sDanaRMessages.put(Integer.valueOf(0x0701), new DanaRMessage("CMD_MSGRECEP_TAKE_SUGAR"));
        sDanaRMessages.put(Integer.valueOf(0x0702), new DanaRMessage("CMD_MSGRECEP_GO_TO_DOCTOR"));
        sDanaRMessages.put(Integer.valueOf(0x0703), new DanaRMessage("CMD_MSGRECEP_CALL_TO_CAREGIVER"));
        sDanaRMessages.put(Integer.valueOf(0x0704), new DanaRMessage("CMD_MSGRECEP_CHECK_GLUCOSE_AGAIN"));
        sDanaRMessages.put(Integer.valueOf(0x0705), new DanaRMessage("CMD_MSGRECEP_CALL_TO_HOME"));
        sDanaRMessages.put(Integer.valueOf(0x0706), new DanaRMessage("CMD_MSGRECEP_DO_DELIVER"));

        sDanaRMessages.put(Integer.valueOf(0x0801), new DanaRMessage("CMD_MSGSEND_YES_I_DO"));
        sDanaRMessages.put(Integer.valueOf(0x0802), new DanaRMessage("CMD_MSGSEND_NO_I_CANNOT"));
        sDanaRMessages.put(Integer.valueOf(0x0803), new DanaRMessage("CMD_MSGSEND_CALL_TO_ME_MOM"));
        sDanaRMessages.put(Integer.valueOf(0x0804), new DanaRMessage("CMD_MSGSEND_DO_NOT_INFUSE"));

        sDanaRMessages.put(Integer.valueOf(0x0901), new DanaRMessage("CMD_FILL_REFILL_COUNT"));
        sDanaRMessages.put(Integer.valueOf(0x0902), new DanaRMessage("CMD_FILL_PRIME_CHECK"));
        sDanaRMessages.put(Integer.valueOf(0x0903), new DanaRMessage("CMD_FILL_PRIME_END"));
        sDanaRMessages.put(Integer.valueOf(0x0904), new DanaRMessage("CMD_FILL_PRIME_STOP"));
        sDanaRMessages.put(Integer.valueOf(0x0905), new DanaRMessage("CMD_FILL_PRIME_PAUSE"));
        sDanaRMessages.put(Integer.valueOf(0x0906), new DanaRMessage("CMD_FILL_PRIME_RATE"));

        sDanaRMessages.put(Integer.valueOf(0x41f2), new MsgHistoryAll("CMD_HISTORY_ALL"));
        sDanaRMessages.put(Integer.valueOf(0x42f2), new DanaRMessage("CMD_HISTORY_NEW"));

        sDanaRMessages.put(Integer.valueOf(0x41f1), new DanaRMessage("CMD_HISTORY_ALL_DONE"));
        sDanaRMessages.put(Integer.valueOf(0x42f1), new DanaRMessage("CMD_HISTORY_NEW_DONE"));

    }

    public static int byteArrayToInt(byte[] bArr,int offset, int lenght) {
        offset+=6;
        switch (lenght) {
            case 1:
                return bArr[0+offset] & 255;
            case 2:
                return ((bArr[0+offset] & 255) << 8) + (bArr[1+offset] & 255);
            case 3:
                return (((bArr[2+offset] & 255) << 16) + ((bArr[1+offset] & 255) << 8)) + (bArr[0+offset] & 255);
            case 4:
                return ((((bArr[3+offset] & 255) << 24) + ((bArr[2+offset] & 255) << 16)) + ((bArr[1+offset] & 255) << 8)) + (bArr[0+offset] & 255);
            default:
                return -1;
        }
    }

    public static Date byteArrayToDateTime(byte[] bArr, int offset) {
        Date date  =
                new Date(
                        100 + DanaRMessages.byteArrayToInt(bArr, offset, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+1, 1) - 1,
                        DanaRMessages.byteArrayToInt(bArr, offset+2, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+3, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+4, 1),
                        0
                );
        return date;
    }

    public static Date byteArrayToDateTimeSec(byte[] bArr, int offset) {
        Date date  =
                new Date(
                        100 + DanaRMessages.byteArrayToInt(bArr, offset, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+1, 1) - 1,
                        DanaRMessages.byteArrayToInt(bArr, offset+2, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+3, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+4, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+5, 1)
                );
        return date;
    }

    public static Date byteArrayToDate(byte[] bArr, int offset) {
        Date date  =
                new Date(
                        100 + DanaRMessages.byteArrayToInt(bArr, offset, 1),
                        DanaRMessages.byteArrayToInt(bArr, offset+1, 1) - 1,
                        DanaRMessages.byteArrayToInt(bArr, offset+2, 1)
                );
        return date;
    }
}
