import csv
import re
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent
CONTEXTO_DIR = BASE_DIR / "contexto"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"

SQL_FILE = CONTEXTO_DIR / "ocs_55_clinicas_sem_null.sql"

COLUNAS_SQL = [
    "ocs_nome", "ocs_inscricao_federal", "ocs_endereco", "ocs_endereco_numero",
    "ocs_endereco_bairro", "ocs_endereco_cidade", "ocs_endereco_uf",
    "ocs_endereco_cep", "ocs_contato_nome", "ocs_contato_telefone", "ocs_especialidade",
]

COLUNAS_SAIDA = [
    "ocs_nome", "ocs_inscricao_federal", "ocs_endereco", "ocs_endereco_numero",
    "ocs_endereco_bairro", "ocs_endereco_cidade", "ocs_endereco_uf",
    "ocs_endereco_cep", "ocs_contato_telefone",
]

FIELD_RE = r"(?:'(?:[^'\\]|\\.)*'|NULL)"
TUPLE_RE = re.compile(r"\(\s*(" + FIELD_RE + r"(?:\s*,\s*" + FIELD_RE + r")*)\s*\)")
FIELD_FINDALL_RE = re.compile(FIELD_RE)


def parse_field(campo):
    campo = campo.strip()
    if campo == "NULL":
        return None
    return campo[1:-1].replace("''", "'")


def extrair_cadastro():
    print("Lendo SQL de cadastro das OCS...")
    texto = SQL_FILE.read_text(encoding="utf-8")

    registros = {}
    for match in TUPLE_RE.finditer(texto):
        campos = FIELD_FINDALL_RE.findall(match.group(1))
        if len(campos) != len(COLUNAS_SQL):
            continue
        valores = dict(zip(COLUNAS_SQL, (parse_field(c) for c in campos)))
        nome = valores["ocs_nome"]
        if nome not in registros:
            registros[nome] = valores

    print(f"  {len(registros)} OCS distintas encontradas")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / "ocs_cadastro.csv"
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=";")
        writer.writerow(COLUNAS_SAIDA)
        for valores in registros.values():
            writer.writerow([valores[c] or "" for c in COLUNAS_SAIDA])

    print(f"  Salvo em {out_path}")


if __name__ == "__main__":
    extrair_cadastro()
    print("Concluido.")
