package br.com.api.walletflow.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {

    @Test
    void reconheceCpfValidoIgnorandoMascara() {
        Document doc = Document.of("111.444.777-35");
        assertInstanceOf(Cpf.class, doc);
        assertEquals("11144477735", doc.value());
    }

    @Test
    void reconheceCnpjValidoIgnorandoMascara() {
        Document doc = Document.of("11.222.333/0001-81");
        assertInstanceOf(Cnpj.class, doc);
        assertEquals("11222333000181", doc.value());
    }

    @Test
    void rejeitaCpfComDigitoVerificadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("11144477700"));
    }

    @Test
    void rejeitaCnpjComDigitoVerificadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("11222333000100"));
    }

    @Test
    void rejeitaCpfComDigitosRepetidos() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("11111111111"));
    }

    @Test
    void rejeitaTamanhoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Document.of("123"));
    }
}
