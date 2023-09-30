package modtools.annotations.builder;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DataColorFieldInit {
	/** 在settingColor中添加 */
	boolean needSetting() default false;
	String data() default "SETTINGS";
	/** 删除字段的前缀 */
	String fieldPrefix() default "";
}
