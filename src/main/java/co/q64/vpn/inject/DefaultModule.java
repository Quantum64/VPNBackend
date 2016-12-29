package co.q64.vpn.inject;

import co.q64.vpn.bind.ConstantBinders.Author;
import co.q64.vpn.bind.ConstantBinders.ModuleName;
import co.q64.vpn.bind.ConstantBinders.Name;
import co.q64.vpn.bind.ConstantBinders.Version;
import co.q64.vpn.util.DefaultConstantPool;

import com.google.inject.AbstractModule;

public class DefaultModule extends AbstractModule {

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Version.class).to(DefaultConstantPool.VERSION);
		bindConstant().annotatedWith(Name.class).to(DefaultConstantPool.NAME);
		bindConstant().annotatedWith(Author.class).to(DefaultConstantPool.AUTHOR);
		bindConstant().annotatedWith(ModuleName.class).to(DefaultConstantPool.DEFAULT_MODULE);
	}
}
