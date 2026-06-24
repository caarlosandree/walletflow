package br.com.api.walletflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Guardião da arquitetura: roda em {@code ./gradlew build} e quebra o build se
 * houver ciclo entre módulos ou acesso ilegal a {@code .internal} alheio.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(WalletflowApplication.class);

    @Test
    void semCiclosNemAcessoIlegalEntreModulos() {
        modules.verify();
    }

    @Test
    void geraDocumentacaoC4PlantUml() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
