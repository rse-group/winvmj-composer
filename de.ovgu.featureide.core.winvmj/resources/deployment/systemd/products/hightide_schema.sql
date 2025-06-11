--
-- PostgreSQL database dump
--

-- Dumped from database version 12.18 (Ubuntu 12.18-0ubuntu0.20.04.1)
-- Dumped by pg_dump version 16.2 (Ubuntu 16.2-1.pgdg20.04+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_role_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_role_comp (
    id integer NOT NULL
);


ALTER TABLE public.auth_role_comp OWNER TO hightide;

--
-- Name: auth_role_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_role_impl (
    allowedpermissions text,
    name character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.auth_role_impl OWNER TO hightide;

--
-- Name: auth_user_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_comp (
    id integer NOT NULL
);


ALTER TABLE public.auth_user_comp OWNER TO hightide;

--
-- Name: auth_user_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_impl (
    allowedpermissions character varying(255),
    email character varying(255),
    name character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.auth_user_impl OWNER TO hightide;

--
-- Name: auth_user_passworded; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_passworded (
    password character varying(255),
    id integer NOT NULL,
    user_id integer
);


ALTER TABLE public.auth_user_passworded OWNER TO hightide;

--
-- Name: auth_user_role_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_role_comp (
    id integer NOT NULL
);


ALTER TABLE public.auth_user_role_comp OWNER TO hightide;

--
-- Name: auth_user_role_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_role_impl (
    id integer NOT NULL,
    authrole integer,
    authuser integer
);


ALTER TABLE public.auth_user_role_impl OWNER TO hightide;

--
-- Name: auth_user_social; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.auth_user_social (
    socialid character varying(255),
    id integer NOT NULL,
    user_id integer
);


ALTER TABLE public.auth_user_social OWNER TO hightide;

--
-- Name: automaticreport_activityreport_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_activityreport_comp (
    id integer NOT NULL
);


ALTER TABLE public.automaticreport_activityreport_comp OWNER TO hightide;

--
-- Name: automaticreport_activityreport_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_activityreport_impl (
    asetnetoawaltahun integer NOT NULL,
    bebandankerugianmanajemendanumum integer NOT NULL,
    bebandankerugianpencariandana integer NOT NULL,
    bebandankerugianprogram integer NOT NULL,
    berakhirnyapembatasanwaktu integer NOT NULL,
    jasalayanan integer NOT NULL,
    kerugianaktuarialdankewajibantahunan integer NOT NULL,
    lainlain integer NOT NULL,
    pembatasan character varying(255),
    pemenuhanpembatasanpemerolehanperalatan integer NOT NULL,
    pemenuhanprogrampembatasan integer NOT NULL,
    penghasilaninvestasijangkapanjang integer NOT NULL,
    penghasilaninvestasilain integer NOT NULL,
    penghasilannetoterealisasikandanbelumterealisasikandariijp integer NOT NULL,
    sumbangan integer NOT NULL,
    id integer NOT NULL,
    periodic_id integer
);


ALTER TABLE public.automaticreport_activityreport_impl OWNER TO hightide;

--
-- Name: automaticreport_financialposition_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_financialposition_comp (
    id integer NOT NULL
);


ALTER TABLE public.automaticreport_financialposition_comp OWNER TO hightide;

--
-- Name: automaticreport_financialposition_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_financialposition_impl (
    asetnetoterikatpermanen integer NOT NULL,
    asetnetoterikattemporer integer NOT NULL,
    asetnetotidakterikat integer NOT NULL,
    asettetap integer NOT NULL,
    investasijangkapanjang integer NOT NULL,
    investasilancar integer NOT NULL,
    kasdansetarakas integer NOT NULL,
    kewajibantahunan integer NOT NULL,
    pendapatanditerimadimukayangdapatdikembalikan integer NOT NULL,
    persediaandanbiayadibayardimuka integer NOT NULL,
    piutangbunga integer NOT NULL,
    piutanglainlain integer NOT NULL,
    propertiinvestasi integer NOT NULL,
    utangdagang integer NOT NULL,
    utangjangkapanjang integer NOT NULL,
    utanglainlain integer NOT NULL,
    utangwesel integer NOT NULL,
    id integer NOT NULL,
    periodic_id integer
);


ALTER TABLE public.automaticreport_financialposition_impl OWNER TO hightide;

--
-- Name: automaticreport_periodic_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_periodic_comp (
    id integer NOT NULL
);


ALTER TABLE public.automaticreport_periodic_comp OWNER TO hightide;

--
-- Name: automaticreport_periodic_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.automaticreport_periodic_impl (
    isactive boolean NOT NULL,
    name character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.automaticreport_periodic_impl OWNER TO hightide;

--
-- Name: chartofaccount_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.chartofaccount_comp (
    id integer NOT NULL
);


ALTER TABLE public.chartofaccount_comp OWNER TO hightide;

--
-- Name: chartofaccount_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.chartofaccount_impl (
    code integer NOT NULL,
    description character varying(255),
    isvisible character varying(255),
    name character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.chartofaccount_impl OWNER TO hightide;

--
-- Name: donation_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.donation_comp (
    id integer NOT NULL
);


ALTER TABLE public.donation_comp OWNER TO hightide;

--
-- Name: donation_confirmation; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.donation_confirmation (
    proofoftransfer text,
    recieveraccount character varying(255),
    senderaccount character varying(255),
    status character varying(255),
    id integer NOT NULL,
    record_id integer
);


ALTER TABLE public.donation_confirmation OWNER TO hightide;

--
-- Name: donation_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.donation_impl (
    amount bigint NOT NULL,
    date character varying(255),
    description character varying(255),
    email character varying(255),
    name character varying(255),
    paymentmethod character varying(255),
    phone character varying(255),
    id integer NOT NULL,
    income_id integer,
    program_idprogram integer
);


ALTER TABLE public.donation_impl OWNER TO hightide;

--
-- Name: financialreport_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.financialreport_comp (
    id integer NOT NULL,
    amount bigint NOT NULL,
    datestamp character varying(255),
    description character varying(255),
    coa_id integer,
    program_idprogram integer
);


ALTER TABLE public.financialreport_comp OWNER TO hightide;

--
-- Name: financialreport_expense; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.financialreport_expense (
    id integer NOT NULL
);


ALTER TABLE public.financialreport_expense OWNER TO hightide;

--
-- Name: financialreport_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.financialreport_impl (
    id integer NOT NULL
);


ALTER TABLE public.financialreport_impl OWNER TO hightide;

--
-- Name: financialreport_income; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.financialreport_income (
    paymentmethod character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.financialreport_income OWNER TO hightide;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: hightide
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.hibernate_sequence OWNER TO hightide;

--
-- Name: program_activity; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.program_activity (
    idprogram integer NOT NULL
);


ALTER TABLE public.program_activity OWNER TO hightide;

--
-- Name: program_comp; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.program_comp (
    idprogram integer NOT NULL,
    description character varying(255),
    executiondate character varying(255),
    logourl text,
    name character varying(255),
    partner character varying(255),
    target character varying(255)
);


ALTER TABLE public.program_comp OWNER TO hightide;

--
-- Name: program_impl; Type: TABLE; Schema: public; Owner: hightide
--

CREATE TABLE public.program_impl (
    description character varying(255),
    executiondate character varying(255),
    logourl text,
    name character varying(255),
    partner character varying(255),
    target character varying(255),
    idprogram integer NOT NULL
);


ALTER TABLE public.program_impl OWNER TO hightide;

--
-- Name: auth_role_comp auth_role_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_role_comp
    ADD CONSTRAINT auth_role_comp_pkey PRIMARY KEY (id);


--
-- Name: auth_role_impl auth_role_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_role_impl
    ADD CONSTRAINT auth_role_impl_pkey PRIMARY KEY (id);


--
-- Name: auth_user_comp auth_user_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_comp
    ADD CONSTRAINT auth_user_comp_pkey PRIMARY KEY (id);


--
-- Name: auth_user_impl auth_user_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_impl
    ADD CONSTRAINT auth_user_impl_pkey PRIMARY KEY (id);


--
-- Name: auth_user_passworded auth_user_passworded_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_passworded
    ADD CONSTRAINT auth_user_passworded_pkey PRIMARY KEY (id);


--
-- Name: auth_user_role_comp auth_user_role_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_role_comp
    ADD CONSTRAINT auth_user_role_comp_pkey PRIMARY KEY (id);


--
-- Name: auth_user_role_impl auth_user_role_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_role_impl
    ADD CONSTRAINT auth_user_role_impl_pkey PRIMARY KEY (id);


--
-- Name: auth_user_social auth_user_social_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_social
    ADD CONSTRAINT auth_user_social_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_activityreport_comp automaticreport_activityreport_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_activityreport_comp
    ADD CONSTRAINT automaticreport_activityreport_comp_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_activityreport_impl automaticreport_activityreport_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_activityreport_impl
    ADD CONSTRAINT automaticreport_activityreport_impl_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_financialposition_comp automaticreport_financialposition_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_financialposition_comp
    ADD CONSTRAINT automaticreport_financialposition_comp_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_financialposition_impl automaticreport_financialposition_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_financialposition_impl
    ADD CONSTRAINT automaticreport_financialposition_impl_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_periodic_comp automaticreport_periodic_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_periodic_comp
    ADD CONSTRAINT automaticreport_periodic_comp_pkey PRIMARY KEY (id);


--
-- Name: automaticreport_periodic_impl automaticreport_periodic_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_periodic_impl
    ADD CONSTRAINT automaticreport_periodic_impl_pkey PRIMARY KEY (id);


--
-- Name: chartofaccount_comp chartofaccount_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.chartofaccount_comp
    ADD CONSTRAINT chartofaccount_comp_pkey PRIMARY KEY (id);


--
-- Name: chartofaccount_impl chartofaccount_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.chartofaccount_impl
    ADD CONSTRAINT chartofaccount_impl_pkey PRIMARY KEY (id);


--
-- Name: donation_comp donation_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_comp
    ADD CONSTRAINT donation_comp_pkey PRIMARY KEY (id);


--
-- Name: donation_confirmation donation_confirmation_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_confirmation
    ADD CONSTRAINT donation_confirmation_pkey PRIMARY KEY (id);


--
-- Name: donation_impl donation_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_impl
    ADD CONSTRAINT donation_impl_pkey PRIMARY KEY (id);


--
-- Name: financialreport_comp financialreport_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_comp
    ADD CONSTRAINT financialreport_comp_pkey PRIMARY KEY (id);


--
-- Name: financialreport_expense financialreport_expense_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_expense
    ADD CONSTRAINT financialreport_expense_pkey PRIMARY KEY (id);


--
-- Name: financialreport_impl financialreport_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_impl
    ADD CONSTRAINT financialreport_impl_pkey PRIMARY KEY (id);


--
-- Name: financialreport_income financialreport_income_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_income
    ADD CONSTRAINT financialreport_income_pkey PRIMARY KEY (id);


--
-- Name: program_activity program_activity_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.program_activity
    ADD CONSTRAINT program_activity_pkey PRIMARY KEY (idprogram);


--
-- Name: program_comp program_comp_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.program_comp
    ADD CONSTRAINT program_comp_pkey PRIMARY KEY (idprogram);


--
-- Name: program_impl program_impl_pkey; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.program_impl
    ADD CONSTRAINT program_impl_pkey PRIMARY KEY (idprogram);


--
-- Name: auth_user_social uk_a5tmedqsrf0frqeb6dcbakrax; Type: CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_social
    ADD CONSTRAINT uk_a5tmedqsrf0frqeb6dcbakrax UNIQUE (socialid);


--
-- Name: auth_user_passworded fk19s1olt8skpbpguobv5ribt6o; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_passworded
    ADD CONSTRAINT fk19s1olt8skpbpguobv5ribt6o FOREIGN KEY (id) REFERENCES public.auth_user_comp(id);


--
-- Name: auth_user_role_impl fk1fdbc1l60nrlf03rubtij4y6a; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_role_impl
    ADD CONSTRAINT fk1fdbc1l60nrlf03rubtij4y6a FOREIGN KEY (id) REFERENCES public.auth_user_role_comp(id);


--
-- Name: financialreport_comp fk3adg4texf9c90cqvp1m7cgyg8; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_comp
    ADD CONSTRAINT fk3adg4texf9c90cqvp1m7cgyg8 FOREIGN KEY (program_idprogram) REFERENCES public.program_comp(idprogram);


--
-- Name: auth_user_role_impl fk3pokxn1i18kalevuka456mp6p; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_role_impl
    ADD CONSTRAINT fk3pokxn1i18kalevuka456mp6p FOREIGN KEY (authuser) REFERENCES public.auth_user_impl(id);


--
-- Name: automaticreport_financialposition_impl fk43ynu6wf42rv5kllyc28mpyyj; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_financialposition_impl
    ADD CONSTRAINT fk43ynu6wf42rv5kllyc28mpyyj FOREIGN KEY (id) REFERENCES public.automaticreport_financialposition_comp(id);


--
-- Name: program_impl fk4d5bk0altx5ilui761k48iq22; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.program_impl
    ADD CONSTRAINT fk4d5bk0altx5ilui761k48iq22 FOREIGN KEY (idprogram) REFERENCES public.program_comp(idprogram);


--
-- Name: automaticreport_periodic_impl fk9gp2y6r4jcmy3ry8oi04n9kdv; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_periodic_impl
    ADD CONSTRAINT fk9gp2y6r4jcmy3ry8oi04n9kdv FOREIGN KEY (id) REFERENCES public.automaticreport_periodic_comp(id);


--
-- Name: automaticreport_financialposition_impl fk9sxpt3rybvl0mb7mryblwesmx; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_financialposition_impl
    ADD CONSTRAINT fk9sxpt3rybvl0mb7mryblwesmx FOREIGN KEY (periodic_id) REFERENCES public.automaticreport_periodic_comp(id);


--
-- Name: financialreport_comp fkaax3odxitklk5bmb8o7ictbo; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_comp
    ADD CONSTRAINT fkaax3odxitklk5bmb8o7ictbo FOREIGN KEY (coa_id) REFERENCES public.chartofaccount_comp(id);


--
-- Name: financialreport_impl fkbjsvgc403auq9n9j9xt5fe8h9; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_impl
    ADD CONSTRAINT fkbjsvgc403auq9n9j9xt5fe8h9 FOREIGN KEY (id) REFERENCES public.financialreport_comp(id);


--
-- Name: chartofaccount_impl fkcj60s5kw2u1t4teov8jbk52ce; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.chartofaccount_impl
    ADD CONSTRAINT fkcj60s5kw2u1t4teov8jbk52ce FOREIGN KEY (id) REFERENCES public.chartofaccount_comp(id);


--
-- Name: donation_impl fkcmyc1veutqnw67d48ba2k3hiq; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_impl
    ADD CONSTRAINT fkcmyc1veutqnw67d48ba2k3hiq FOREIGN KEY (id) REFERENCES public.donation_comp(id);


--
-- Name: auth_user_social fkeyexhqdg9y496fqp0u9l4l0uc; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_social
    ADD CONSTRAINT fkeyexhqdg9y496fqp0u9l4l0uc FOREIGN KEY (user_id) REFERENCES public.auth_user_comp(id);


--
-- Name: financialreport_income fkfyrib08v86hhq951oxxhrlfuk; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_income
    ADD CONSTRAINT fkfyrib08v86hhq951oxxhrlfuk FOREIGN KEY (id) REFERENCES public.financialreport_comp(id);


--
-- Name: auth_role_impl fkg93esbm013a0au2sck1jwa1be; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_role_impl
    ADD CONSTRAINT fkg93esbm013a0au2sck1jwa1be FOREIGN KEY (id) REFERENCES public.auth_role_comp(id);


--
-- Name: automaticreport_activityreport_impl fkhxamifkoo5nne7d17svp09p8f; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_activityreport_impl
    ADD CONSTRAINT fkhxamifkoo5nne7d17svp09p8f FOREIGN KEY (id) REFERENCES public.automaticreport_activityreport_comp(id);


--
-- Name: auth_user_impl fkj93qld8dfmwxxethtnkbs0p0p; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_impl
    ADD CONSTRAINT fkj93qld8dfmwxxethtnkbs0p0p FOREIGN KEY (id) REFERENCES public.auth_user_comp(id);


--
-- Name: auth_user_passworded fkl3tsngvir2naifbhumm0v6rqd; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_passworded
    ADD CONSTRAINT fkl3tsngvir2naifbhumm0v6rqd FOREIGN KEY (user_id) REFERENCES public.auth_user_comp(id);


--
-- Name: financialreport_expense fklpguobmd4m7bc64p5tbsc8466; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.financialreport_expense
    ADD CONSTRAINT fklpguobmd4m7bc64p5tbsc8466 FOREIGN KEY (id) REFERENCES public.financialreport_comp(id);


--
-- Name: donation_confirmation fklwjdhdkjmv3v1lhqrhp7ip7kt; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_confirmation
    ADD CONSTRAINT fklwjdhdkjmv3v1lhqrhp7ip7kt FOREIGN KEY (id) REFERENCES public.donation_comp(id);


--
-- Name: auth_user_social fkoape53p3lif2celki8tiu8fki; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_social
    ADD CONSTRAINT fkoape53p3lif2celki8tiu8fki FOREIGN KEY (id) REFERENCES public.auth_user_comp(id);


--
-- Name: program_activity fkoqgjbm4q6rqnxq51yiylwh8si; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.program_activity
    ADD CONSTRAINT fkoqgjbm4q6rqnxq51yiylwh8si FOREIGN KEY (idprogram) REFERENCES public.program_comp(idprogram);


--
-- Name: donation_impl fkp0vq00gimjl8qpt4wbmtecbra; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_impl
    ADD CONSTRAINT fkp0vq00gimjl8qpt4wbmtecbra FOREIGN KEY (income_id) REFERENCES public.financialreport_comp(id);


--
-- Name: auth_user_role_impl fkrkludg4ww825oy1pal92rhett; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.auth_user_role_impl
    ADD CONSTRAINT fkrkludg4ww825oy1pal92rhett FOREIGN KEY (authrole) REFERENCES public.auth_role_comp(id);


--
-- Name: donation_confirmation fksnsvoa531cjp0daaklgx0bjxh; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_confirmation
    ADD CONSTRAINT fksnsvoa531cjp0daaklgx0bjxh FOREIGN KEY (record_id) REFERENCES public.donation_comp(id);


--
-- Name: automaticreport_activityreport_impl fktas69jv64v5wsqh86gbt0vacd; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.automaticreport_activityreport_impl
    ADD CONSTRAINT fktas69jv64v5wsqh86gbt0vacd FOREIGN KEY (periodic_id) REFERENCES public.automaticreport_periodic_comp(id);


--
-- Name: donation_impl fky8kujypbtyus61wodkpovj1t; Type: FK CONSTRAINT; Schema: public; Owner: hightide
--

ALTER TABLE ONLY public.donation_impl
    ADD CONSTRAINT fky8kujypbtyus61wodkpovj1t FOREIGN KEY (program_idprogram) REFERENCES public.program_comp(idprogram);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

