package com.gitihub.xdering.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringStyle;

public class DateToStringStyle extends ToStringStyle {
	private static final long serialVersionUID = -8508974112614626337L;
	public static final ToStringStyle DEFAULT_STYLE    = new DateStyle("yyyy-MM-dd HH:mm:ss");

     private static class DateStyle extends ToStringStyle {

        private static final long serialVersionUID = 2027017652230379965L;
        private String            datePattern;

         public DateStyle(String datePattern){
             super();
             this.setUseIdentityHashCode(false);
             this.setUseShortClassName(true);
             this.datePattern = datePattern;
         }

         protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
             if (value instanceof Date) {
                 value = new SimpleDateFormat(datePattern).format(value);
             }
             buffer.append(value);
         }
     }
}