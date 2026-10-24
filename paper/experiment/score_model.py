# Python port of EsgScoreCalculator (verified against the Java unit tests below)
IND = {  # E, S, G
 "Energy":(.45,.22,.33),"Materials":(.45,.22,.33),"Utilities":(.45,.22,.33),"Industrials":(.35,.32,.33),
 "Consumer Discretionary":(.30,.37,.33),"Consumer Staples":(.35,.32,.33),"Health Care":(.15,.52,.33),
 "Financials":(.12,.40,.48),"Information Technology":(.15,.52,.33),"Communication Services":(.10,.55,.35),"Real Estate":(.40,.27,.33)}
ISSUE = {"A":("G",),"B":("G",),"P1":("G",),"P7":("G",),"P2":("E",),"P6":("E",),"P3":("S",),"P4":("S",),"P5":("S",),"P8":("S",),"P9":("S",)}
PIL = {"A":"G","B":"G","P1":"G","P7":"G","P2":"E","P6":"E","P3":"S","P4":"S","P5":"S","P8":"S","P9":"S"}
GROUP = {"A":"AB","B":"AB"}
def key(sec): return "AB" if sec in ("A","B") else sec
def calc(industry, items):
    # items: list of (section, indicator, status, count)
    t={}
    for sec,ind,st,n in items:
        k=key(sec); d=t.setdefault(k,[0.0,0.0])
        if st in ("C","P","N"):
            w=0.5 if ind=="L" else 1.0
            d[1]+=w*n; d[0]+=w*n*{"C":10,"P":5,"N":0}[st]
    score={k:(v[0]/v[1]) for k,v in t.items() if v[1]>0}
    pil={}
    for k,s in score.items():
        pil.setdefault(PIL["A" if k=="AB" else k],[]).append(s)
    base=dict(zip("ESG",IND[industry] if industry else (1/3,)*3))
    act=sum(base[p] for p in pil)
    pw={p:base[p]/act for p in pil}
    w=sum(pw[p]/len(pil[p])*s for p in pil for s in pil[p])
    return w,{p:sum(v)/len(v) for p,v in pil.items()}
SECS=["A","B","P1","P2","P3","P4","P5","P6","P7","P8","P9"]
def everything(st): return [(s,"E",st,4) for s in SECS]
BANDS=["CCC","B","BB","BBB","A","AA","AAA"]
def band(x): return BANDS[max(0,min(6,int((x+1e-9)//(10/7))))]
if __name__=="__main__":
    assert abs(calc("Energy",everything("C"))[0]-10)<1e-9
    inputs=[(s,"E","N",4) for s in SECS if s not in("P2","P6")]+[("P2","E","C",4),("P6","E","C",4)]
    assert abs(calc("Energy",inputs)[0]-4.5)<1e-9 and abs(calc("Information Technology",inputs)[0]-1.5)<1e-9
    assert abs(calc(None,[("P6","E","C",1),("P6","L","N",1)])[0]-6.6667)<1e-3
    print("port matches Java tests")
    for ind in IND:
        env=[(s,"E","C" if PIL[s]=="E" else "N",4) for s in SECS]
        soc=[(s,"E","C" if PIL[s]=="S" else "N",4) for s in SECS]
        gov=[(s,"E","C" if PIL[s]=="G" else "N",4) for s in SECS]
        print(ind, round(calc(ind,env)[0],2), round(calc(ind,soc)[0],2), round(calc(ind,gov)[0],2))
    # N/A scenario: P6 = 21 essential-weight reqs (use 12 ess + 9 lead), 6 essential not covered
    def p6(st6):
        it=[(s,"E","C",4) for s in SECS if s!="P6"]
        it+= [("P6","E","C",6),("P6","E",st6,6),("P6","L","C",9)]
        return it
    for st in ("N","X"):
        print("P6 6 ess ", st, [round(calc(i,p6(st))[0],3) for i in ("Energy","Information Technology")])
    print("full-coverage p6 base", round(calc("Energy",everything("C"))[0],2))
    print(band(5.0),band(3.75),band(10),band(0))
