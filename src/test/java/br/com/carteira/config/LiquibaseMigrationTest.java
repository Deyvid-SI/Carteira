package br.com.carteira.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LiquibaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveCriarSchemaVersionadoAntesDoHibernate() {
        Integer tabelasDaAplicacao = jdbcTemplate.queryForObject("""
                select count(*)
                  from information_schema.tables
                 where table_schema = 'PUBLIC'
                   and table_name in ('USUARIOS', 'CORRETORAS', 'ACOES', 'HISTORICOS_COTACAO', 'NEGOCIACOES')
                """, Integer.class);

        Integer changeSetsExecutados = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog", Integer.class);

        assertThat(tabelasDaAplicacao).isEqualTo(5);
        assertThat(changeSetsExecutados).isEqualTo(5);
    }
}
