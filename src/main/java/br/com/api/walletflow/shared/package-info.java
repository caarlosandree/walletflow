/**
 * Shared kernel: value objects reutilizados pelos demais módulos
 * ({@link br.com.api.walletflow.shared.Money}, {@link br.com.api.walletflow.shared.Document}).
 *
 * <p>Declarado como módulo OPEN para que os outros módulos possam depender
 * livremente destes tipos sem violar as regras de fronteira do Modulith.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package br.com.api.walletflow.shared;
