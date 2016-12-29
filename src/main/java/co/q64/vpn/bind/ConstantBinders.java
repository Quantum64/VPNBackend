package co.q64.vpn.bind;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

public class ConstantBinders {
	//formatter:off
	@Target({ ElementType.FIELD })
	@Retention(RUNTIME) @BindingAnnotation
	public static @interface Author {}

	@Target({ ElementType.FIELD })
	@Retention(RUNTIME) @BindingAnnotation
	public static @interface ModuleName {}

	@Target({ ElementType.FIELD })
	@Retention(RUNTIME) @BindingAnnotation
	public static @interface Name {}

	@Target({ ElementType.FIELD })
	@Retention(RUNTIME) @BindingAnnotation
	public static @interface Version {}
	//formatter:on
}
