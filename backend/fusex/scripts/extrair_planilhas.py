import re
import unicodedata
import pandas as pd
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent
CONTEXTO_DIR = BASE_DIR / "contexto"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"

TUSS_FILE = CONTEXTO_DIR / "rol_tuss_rol_simplificado_rn305tuss_alterada_pela_rn_349_2014_ajustada_site_11-03-2015.xlsx"
PRECOS_FILE = CONTEXTO_DIR / "TABELA DE PARÂMETROS DE PREÇOS - UG-FUSEX.xls"

ESPECIALIDADE_MAP = {
    "cardiologia": "CARDIOLOGISTA",
    "ortopedia": "ORTOPEDISTA",
    "endocrinologia": "ENDOCRINOLOGISTA",
    "ginecologia": "GINECOLOGISTA_OBSTETRA",
    "obstetricia": "GINECOLOGISTA_OBSTETRA",
    "ginecologia e obstetricia": "GINECOLOGISTA_OBSTETRA",
    "nutricao": "NUTRICIONISTA",
    "psiquiatria": "PSIQUIATRA",
    "dermatologia": "DERMATOLOGISTA",
}


def normalizar(texto):
    if texto is None or (isinstance(texto, float)):
        return ""
    texto = str(texto).strip().lower()
    texto = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode("ascii")
    texto = re.sub(r"\s+", " ", texto)
    return texto


def mapear_especialidade(nome_planilha):
    chave = normalizar(nome_planilha)
    return ESPECIALIDADE_MAP.get(chave, "OUTROS")


def extrair_tuss():
    print("Lendo Rol TUSS...")
    df = pd.read_excel(TUSS_FILE, sheet_name=0, header=1)
    df = df.iloc[:, [0, 1]]
    df.columns = ["proc_codigo_dgp", "proc_descricao"]
    df = df.dropna(subset=["proc_codigo_dgp", "proc_descricao"])
    df["proc_codigo_dgp"] = df["proc_codigo_dgp"].astype(str).str.strip()
    df["proc_descricao"] = df["proc_descricao"].astype(str).str.strip()
    df = df[df["proc_codigo_dgp"].str.match(r"^\d+$")]
    antes = len(df)
    df = df.drop_duplicates(subset=["proc_codigo_dgp"], keep="first")
    print(f"  {antes} linhas -> {len(df)} codigos unicos")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / "tuss_procedimentos.csv"
    df.to_csv(out_path, sep=";", index=False, encoding="utf-8")
    print(f"  Salvo em {out_path}")

    indice = {}
    for _, row in df.iterrows():
        chave = normalizar(row["proc_descricao"])
        if chave and chave not in indice:
            indice[chave] = row["proc_codigo_dgp"]
    return indice


def achar_linha_com_texto(df, texto, coluna=0, a_partir_de=0):
    alvo = normalizar(texto)
    for i in range(a_partir_de, len(df)):
        valor = normalizar(df.iat[i, coluna]) if coluna < df.shape[1] else ""
        if alvo in valor:
            return i
    return None


def extrair_planilha_ocs(nome_aba, df, indice_tuss, contador_local):
    linhas_saida = []

    linha_tabela1 = achar_linha_com_texto(df, "Tabela 1")
    if linha_tabela1 is None:
        print(f"  [AVISO] '{nome_aba}': nao achei 'Tabela 1', pulando aba")
        return linhas_saida, contador_local

    linha_header1 = linha_tabela1 + 2
    linha_dados1 = linha_header1 + 1
    try:
        ocs_nome = str(df.iat[linha_dados1, 0]).strip()
        ocs_tipo = str(df.iat[linha_dados1, 1]).strip().upper()
        ocs_contrato = df.iat[linha_dados1, 2]
        ocs_inicio = df.iat[linha_dados1, 3]
        ocs_termino = df.iat[linha_dados1, 4]
    except Exception as e:
        print(f"  [AVISO] '{nome_aba}': erro lendo Tabela 1 ({e}), pulando aba")
        return linhas_saida, contador_local

    if not ocs_nome or ocs_nome.lower() == "nan":
        print(f"  [AVISO] '{nome_aba}': nome da OCS vazio, pulando aba")
        return linhas_saida, contador_local

    ocs_tipo = "PSA" if "PSA" in ocs_tipo else "OCS"
    ocs_contrato = "" if pd.isna(ocs_contrato) else str(ocs_contrato).strip()
    ocs_inicio_str = "" if pd.isna(ocs_inicio) else pd.to_datetime(ocs_inicio).strftime("%Y-%m-%d")
    ocs_termino_str = "" if pd.isna(ocs_termino) else pd.to_datetime(ocs_termino).strftime("%Y-%m-%d")

    linha_especialidades_label = achar_linha_com_texto(df, "Especialidades", a_partir_de=linha_dados1)
    especialidades = []
    if linha_especialidades_label is not None:
        i = linha_especialidades_label + 1
        while i < len(df):
            valor = df.iat[i, 0]
            if pd.isna(valor) or str(valor).strip() == "":
                break
            if "tabela 2" in normalizar(valor):
                break
            especialidades.append(mapear_especialidade(valor))
            i += 1
    especialidades = sorted(set(especialidades)) or ["OUTROS"]
    especialidades_str = ",".join(especialidades)

    linha_tabela2 = achar_linha_com_texto(df, "Tabela 2", a_partir_de=linha_dados1)
    if linha_tabela2 is None:
        print(f"  [AVISO] '{nome_aba}': nao achei 'Tabela 2', só contrato+especialidades salvos")
        return linhas_saida, contador_local

    linha_header2 = achar_linha_com_texto(df, "Nr Ordem", a_partir_de=linha_tabela2)
    if linha_header2 is None:
        print(f"  [AVISO] '{nome_aba}': nao achei header 'Nr Ordem' da Tabela 2")
        return linhas_saida, contador_local

    descricao_atual = ""
    i = linha_header2 + 1
    while i < len(df):
        nr_ordem = df.iat[i, 0]
        if pd.isna(nr_ordem) and pd.isna(df.iat[i, 2]) and pd.isna(df.iat[i, 4]):
            break

        descricao_cel = df.iat[i, 1] if df.shape[1] > 1 else None
        if not pd.isna(descricao_cel) and str(descricao_cel).strip():
            descricao_atual = str(descricao_cel).strip()

        procedimento_nome = df.iat[i, 2] if df.shape[1] > 2 else None
        valor = df.iat[i, 4] if df.shape[1] > 4 else None
        tabela_ref = df.iat[i, 5] if df.shape[1] > 5 else None

        if pd.isna(procedimento_nome) or str(procedimento_nome).strip() == "":
            i += 1
            continue
        procedimento_nome = str(procedimento_nome).strip()

        if pd.isna(valor):
            i += 1
            continue
        try:
            valor_num = float(valor)
        except (TypeError, ValueError):
            i += 1
            continue

        chave_normalizada = normalizar(procedimento_nome)
        codigo = indice_tuss.get(chave_normalizada)
        origem = "TUSS"
        if codigo is None:
            contador_local += 1
            codigo = f"LOCAL-{contador_local:06d}"
            origem = "OCS_LOCAL"

        tabela_ref_str = "" if pd.isna(tabela_ref) else str(tabela_ref).strip()

        linhas_saida.append({
            "ocs_nome": ocs_nome,
            "ocs_tipo": ocs_tipo,
            "ocs_contrato_numero": ocs_contrato,
            "ocs_inicio_vigencia": ocs_inicio_str,
            "ocs_termino_vigencia": ocs_termino_str,
            "especialidades": especialidades_str,
            "proc_codigo_dgp": codigo,
            "proc_origem": origem,
            "proc_descricao": procedimento_nome,
            "valor": valor_num,
            "tabela_referencia": tabela_ref_str,
            "descricao_grupo": descricao_atual,
        })
        i += 1

    return linhas_saida, contador_local


def extrair_precos(indice_tuss):
    print("Lendo planilha de precos por OCS...")
    xls = pd.ExcelFile(PRECOS_FILE)
    abas_reais = [
        s for s in xls.sheet_names
        if not s.startswith("Planilha") and normalizar(s) != "inicio"
    ]
    print(f"  {len(abas_reais)} abas reais de {len(xls.sheet_names)} totais")

    todas_linhas = []
    contador_local = 0
    for aba in abas_reais:
        df = pd.read_excel(xls, sheet_name=aba, header=None)
        linhas, contador_local = extrair_planilha_ocs(aba, df, indice_tuss, contador_local)
        print(f"  '{aba}': {len(linhas)} linhas de preco extraidas")
        todas_linhas.extend(linhas)

    df_saida = pd.DataFrame(todas_linhas)
    out_path = OUTPUT_DIR / "ocs_precos.csv"
    df_saida.to_csv(out_path, sep=";", index=False, encoding="utf-8")
    print(f"Total: {len(df_saida)} linhas de preco salvas em {out_path}")
    print(f"OCS distintas: {df_saida['ocs_nome'].nunique() if len(df_saida) else 0}")
    print(f"Procedimentos LOCAL_ (sem match TUSS): {(df_saida['proc_origem'] == 'OCS_LOCAL').sum() if len(df_saida) else 0}")


if __name__ == "__main__":
    indice_tuss = extrair_tuss()
    extrair_precos(indice_tuss)
    print("Concluido.")
